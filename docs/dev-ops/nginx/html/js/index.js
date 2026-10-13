/* =====================================================================
 * s-pay-mall 商品详情页（拼团下单）
 *
 *   数据展示逻辑整体合并自 group-buy-market 首页（js/index.js）：
 *     轮播图 / 商品标题与活动状态 / 优惠条 / 正在拼的团（团长、成员、进度、倒计时）/
 *     拼团统计 / 活动未开始或已结束时的置灰与原因提示
 *
 *   下单与支付改成走 s-pay-mall（本项目）：
 *     点「单独购买」          → POST {sPayMallUrl}/api/v1/alipay/create_pay_order
 *                               { userId, productId, marketType: 0 }
 *     点「开团购买」          → 同一个接口，{ userId, productId, teamId: null, activityId, marketType: 1 }
 *     点「立即抢单 / 继续支付」→ 同一个接口，带上该团的 teamId、marketType: 1
 *     拿到返回的支付宝表单 → 弹窗确认（优惠没生效时提示按原价支付）→ 提交表单跳沙箱收银台
 *
 *   拼团信息查询仍然走拼团系统 query_group_buy_market_config。
 * ===================================================================== */

/** 纯 UI 预览：地址栏加 ?demo=1 时，接口失败就用本地示例数据渲染 */
const DEMO_MODE = new URLSearchParams(location.search).has('demo');

/** 活动ID兜底：接口没返回 activityId 时用本项目测试活动（group_buy_activity 100123） */
const FALLBACK_ACTIVITY_ID = 100123;

/** 商品图（拼团系统同一商品的图，s-pay-mall 这里直接复用） */
const GOODS_PRESET = {
    goodsName: '《手写MyBatis：渐进式源码实践》',
    goodsThumb: 'image/sku-13811216-01.png',
    carousel: [
        'image/sku-13811216-01.png',
        'image/sku-13811216-02.png',
        'image/sku-13811216-03.png',
        'image/sku-13811216-04.png'
    ]
};

/** 接口不可用时的兜底展示（活动时间取"当前时间前后"，方便看状态效果） */
const FALLBACK_DATA = {
    goods: {
        goodsId: GOODS_ID,
        goodsName: GOODS_PRESET.goodsName,
        originalPrice: 100,
        deductionPrice: 10,
        payPrice: 90,
        startTime: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        endTime: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString()
    },
    teamList: [],
    teamStatistic: { allTeamCount: 0, getTeamCompleteCount: 0, allTeamUserCount: 0 }
};

const CAROUSEL_INTERVAL = 3000;
let carouselIndex = 0;
let carouselTimer = null;
const teamTimers = [];

/** 活动状态：ongoing | upcoming | ended | ''（接口未返回活动时间） */
let currentActivityState = '';
let activityStart = null;
let activityEnd = null;
let activityTimer = null;

/** 优惠条文案用的价格快照 */
let benefitSnapshot = { deductionPrice: 0, targetCount: 2 };
/** 当前活动ID（优先取接口返回的） */
let currentActivityId = FALLBACK_ACTIVITY_ID;
/** 价格快照，供下单时兜底展示 */
let priceSnapshot = { payPrice: 0, originalPrice: 0 };

/* ---------------------- 页面初始化 ---------------------- */
document.addEventListener('DOMContentLoaded', function () {
    const userId = requireLogin();
    if (!userId) return;

    const userNameEl = document.getElementById('userName');
    if (userNameEl) userNameEl.innerText = '欢迎，' + obfuscateUserId(userId);
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);

    renderCarousel();
    renderGoodsPreset();
    bindActions();
    loadMarketConfig();

    // 取消支付 → 静默刷新列表（订单可能已锁，按钮要变成「继续支付」）
    window.onPayCancel = function () {
        loadMarketConfig({ silent: true });
    };
});

/* ---------------------- 轮播 / 商品占位 ---------------------- */
function renderCarousel() {
    const wrapper = document.getElementById('carouselWrapper');
    const dotsBox = document.getElementById('carouselDots');
    if (!wrapper || !dotsBox) return;

    wrapper.innerHTML = GOODS_PRESET.carousel.map(function (src, index) {
        return '<div class="carousel-slide"><img src="' + src + '" alt="商品图 ' + (index + 1) + '"></div>';
    }).join('');
    dotsBox.innerHTML = GOODS_PRESET.carousel.map(function (_, index) {
        return '<span class="dot' + (index === 0 ? ' active' : '') + '"></span>';
    }).join('');

    const dots = dotsBox.querySelectorAll('.dot');
    const total = GOODS_PRESET.carousel.length;

    clearInterval(carouselTimer);
    carouselTimer = setInterval(function () {
        carouselIndex = (carouselIndex + 1) % total;
        wrapper.style.transform = 'translateX(-' + (carouselIndex * 100) + '%)';
        dots.forEach(function (dot, index) {
            dot.classList.toggle('active', index === carouselIndex);
        });
    }, CAROUSEL_INTERVAL);
}

/** 商品名/图先用本地兜底值渲染，接口回来再覆盖 */
function renderGoodsPreset() {
    const thumb = document.getElementById('goodsThumb');
    if (thumb) thumb.src = GOODS_PRESET.goodsThumb;
    setGoodsName(GOODS_PRESET.goodsName);

    const goodsIdEl = document.getElementById('goodsIdText');
    if (goodsIdEl) goodsIdEl.innerText = GOODS_ID;

    // 底部按钮先给出固定文案，保证接口没回来时「下单按钮」也是可见可点的
    setActionText('单独购买', '开团购买');
}

function setGoodsName(name) {
    const el = document.getElementById('goodsName');
    if (el) el.innerText = name || GOODS_PRESET.goodsName;
}

/* ---------------------- 底部操作按钮 ---------------------- */
function setActionText(singleText, groupText) {
    const singleBtn = document.getElementById('singleBuyBtn');
    const groupBtn = document.getElementById('groupBuyBtn');
    // 文案更新后清掉 busy 前的缓存，避免下次恢复成旧价格
    if (singleBtn) { singleBtn.innerText = singleText; delete singleBtn.dataset.originText; }
    if (groupBtn) { groupBtn.innerText = groupText; delete groupBtn.dataset.originText; }
}

function bindActions() {
    // 单独购买：marketType = 0，不透传 teamId / activityId
    const singleBtn = document.getElementById('singleBuyBtn');
    if (singleBtn) {
        singleBtn.addEventListener('click', function () {
            doOrder('', false);
        });
    }

    // 开团购买：marketType = 1，teamId 为空由 s-pay-mall 侧发起营销锁单（新建团）
    const groupBtn = document.getElementById('groupBuyBtn');
    if (groupBtn) {
        groupBtn.addEventListener('click', function () {
            doOrder('', true);
        });
    }
}

/* =====================================================================
 * 下单：统一走 s-pay-mall 的 /api/v1/alipay/create_pay_order
 * @param {string} teamId    空 = 自己开团；有值 = 加入这个团
 * @param {boolean} isGroupBuy true = 拼团单（marketType=1），false = 单独购买（marketType=0）
 * ===================================================================== */
function doOrder(teamId, isGroupBuy) {
    const userId = currentUserId();
    if (!userId) return;

    const price = isGroupBuy ? priceSnapshot.payPrice : priceSnapshot.originalPrice;
    setBuyBarBusy(true, isGroupBuy ? (teamId ? '参团中…' : '开团中…') : '下单中…');
    showToast('正在创建支付单，请稍候…', 'info');

    const body = isGroupBuy
        ? {
            userId: userId,
            productId: GOODS_ID,
            teamId: teamId || null,
            activityId: currentActivityId,
            marketType: MARKET_TYPE.GROUP_BUY_MARKET
        }
        : {
            userId: userId,
            productId: GOODS_ID,
            marketType: MARKET_TYPE.NO_MARKET
        };

    createPayOrder(body)
        .then(function (res) {
            if (!res || res.code !== '0000') {
                hideToast();
                showToast((isGroupBuy ? '拼团下单失败：' : '下单失败：') +
                    ((res && (res.info || res.code)) || '未知错误'), 'error');
                return;
            }

            const data = res.data || {};
            // 兼容两种结构：新结构是对象（带营销信息），旧结构直接是支付宝表单 HTML
            const formHtml = (typeof data === 'string') ? data : data.payUrl;
            if (!formHtml) {
                hideToast();
                showToast('下单失败：支付表单为空，请重新下单', 'error');
                return;
            }

            hideToast();
            openPayModal({
                amount: toNumber(data.payAmount) || price,
                marketFallback: data.marketFallback === true,
                formHtml: formHtml,
                orderId: data.orderId,
                teamId: teamId || ''
            });

            // 下单成功后静默刷新：让列表里的我变成「我的团 · 待支付 / 继续支付」
            loadMarketConfig({ silent: true });
            // 新订单也要立刻出现在「我的订单」里
            if (typeof window.refreshOrders === 'function') window.refreshOrders();
        })
        .catch(function (err) {
            console.error('下单失败:', err);
            hideToast();
            showToast('下单请求失败：' + err.message +
                '（请确认 s-pay-mall 已启动：' + sPayMallUrl + '）', 'error');
        })
        .then(function () {
            setBuyBarBusy(false);
        });
}

/** 下单过程中锁住底部两个按钮，避免重复提交 */
function setBuyBarBusy(busy, text) {
    ['singleBuyBtn', 'groupBuyBtn'].forEach(function (id) {
        const button = document.getElementById(id);
        if (!button) return;
        if (busy) {
            button.dataset.originText = button.dataset.originText || button.innerText;
            button.disabled = true;
            if (text) button.innerText = text;
        } else {
            button.disabled = false;
            if (button.dataset.originText) button.innerText = button.dataset.originText;
        }
    });
}

/* ---------------------- 拉取拼团营销配置（走拼团系统） ---------------------- */
function loadMarketConfig(options) {
    const silent = !!(options && options.silent);

    queryMarketConfig()
        .then(function (res) {
            if (res && res.code === '0000' && res.data) {
                applyMarketData(res.data, false);
                return;
            }
            if (silent) {
                console.warn('静默刷新失败:', res && res.info);
                return;
            }
            handleLoadFailure((res && (res.info || res.code)) || '接口返回异常');
        })
        .catch(function (err) {
            console.warn('拼团信息接口调用失败:', err);
            if (silent) return;
            handleLoadFailure('拼团系统未连接' + '（' + groupBuyMarketUrl + '）');
        });
}

/** 接口失败：?demo=1 用示例数据看 UI，否则只提示失败原因，不编造数据 */
function handleLoadFailure(reason) {
    if (DEMO_MODE) {
        applyMarketData(FALLBACK_DATA, true, reason);
        return;
    }
    renderLoadError(reason);
}

function renderLoadError(reason) {
    stopActivityTimer();
    currentActivityState = '';

    const tip = document.getElementById('dataSourceTip');
    if (tip) {
        tip.hidden = false;
        tip.innerHTML = '拼团数据加载失败：' + escapeHtml(reason) +
            ' <span class="tip-retry" id="retryBtn">重试</span>';
        const retryBtn = document.getElementById('retryBtn');
        if (retryBtn) retryBtn.onclick = function () { loadMarketConfig(); };
    }

    const statusEl = document.getElementById('activityStatus');
    const timeEl = document.getElementById('activityTime');
    if (statusEl) statusEl.hidden = true;
    if (timeEl) timeEl.hidden = true;

    setText('singlePrice', '--');
    setText('groupPrice', '--');
    const saveEl = document.getElementById('saveAmount');
    if (saveEl) saveEl.hidden = true;
    setText('benefitTag', '加载失败');
    setText('benefitText', '点击上方「重试」重新加载');

    teamTimers.forEach(function (timer) { clearInterval(timer); });
    teamTimers.length = 0;
    const list = document.getElementById('memberList');
    if (list) list.innerHTML = '<div class="team-empty">拼团数据加载失败，暂时无法显示正在拼的团</div>';
    setText('teamStatistic', '');

    // 按钮保持可见可点：单独购买不依赖拼团数据，开团购买用兜底活动ID
    setActionText('单独购买', '开团购买');
    setBuyEnabled(true);
}

function applyMarketData(data, isFallback, reason) {
    const goods = data.goods || FALLBACK_DATA.goods;
    const teamList = data.teamList || [];
    const statistic = data.teamStatistic || {};

    // 活动ID：优先用接口顶层返回的，其次取团里的，最后用兜底值
    if (data.activityId) {
        currentActivityId = data.activityId;
    } else if (teamList.length && teamList[0].activityId) {
        currentActivityId = teamList[0].activityId;
    }

    renderGoods(goods, teamList);
    renderTeams(teamList, isFallback);
    renderStatistic(statistic, teamList);

    const tip = document.getElementById('dataSourceTip');
    if (tip) {
        if (isFallback) {
            tip.hidden = false;
            tip.innerText = '当前展示示例数据（' + (reason || '接口不可用') + '）';
        } else {
            tip.hidden = true;
        }
    }
}

/* ---------------------- 商品价格 / 优惠条 ---------------------- */
function renderGoods(goods, teamList) {
    const originalPrice = toNumber(goods.originalPrice);
    const payPrice = toNumber(goods.payPrice);
    const deductionPrice = toNumber(goods.deductionPrice);
    const targetCount = (teamList[0] && teamList[0].targetCount) || 2;

    priceSnapshot = { payPrice: payPrice, originalPrice: originalPrice };

    setText('singlePrice', formatPrice(originalPrice));
    setText('groupPrice', formatPrice(payPrice));
    setActionText(
        '单独购买(￥' + formatPrice(originalPrice) + ')',
        '开团购买(￥' + formatPrice(payPrice) + ')'
    );

    const saveEl = document.getElementById('saveAmount');
    if (saveEl) {
        if (deductionPrice > 0) {
            saveEl.hidden = false;
            saveEl.innerText = '立省 ¥' + formatPrice(deductionPrice);
        } else {
            saveEl.hidden = true;
        }
    }

    setText('goodsIdText', goods.goodsId || GOODS_ID);
    setGoodsName(goods.goodsName);

    benefitSnapshot = { deductionPrice: deductionPrice, targetCount: targetCount };
    renderActivityTime(goods);
    renderBenefitText();
}

/* ---------------------- 活动时间 / 状态 ---------------------- */
function renderActivityTime(goods) {
    stopActivityTimer();

    activityStart = parseDate(goods.startTime);
    activityEnd = parseDate(goods.endTime);

    if (!activityStart && !activityEnd) {
        const statusEl = document.getElementById('activityStatus');
        const timeEl = document.getElementById('activityTime');
        if (statusEl) statusEl.hidden = true;
        if (timeEl) timeEl.hidden = true;
        currentActivityState = '';
        applyActivityGate();
        return;
    }

    renderActivityBadge();
    activityTimer = setInterval(renderActivityBadge, 1000);
}

function renderActivityBadge() {
    if (!activityStart && !activityEnd) return;

    const statusEl = document.getElementById('activityStatus');
    const timeEl = document.getElementById('activityTime');
    const now = Date.now();

    let status = 'ongoing';
    if (activityStart && now < activityStart.getTime()) {
        status = 'upcoming';
    } else if (activityEnd && now > activityEnd.getTime()) {
        status = 'ended';
    }

    const changed = status !== currentActivityState;
    currentActivityState = status;

    if (statusEl) {
        statusEl.hidden = false;
        statusEl.className = 'activity-status ' + status;
        statusEl.innerText = { ongoing: '进行中', upcoming: '即将开始', ended: '已结束' }[status];
    }
    if (timeEl) {
        timeEl.hidden = false;
        timeEl.innerText = '活动时间 ' + formatDateTime(activityStart) + ' ~ ' + formatDateTime(activityEnd) +
            buildRemainText(status, now);
    }

    if (changed) {
        applyActivityGate();
        renderBenefitText();
    }
}

function stopActivityTimer() {
    if (activityTimer) {
        clearInterval(activityTimer);
        activityTimer = null;
    }
}

/** 剩余时间文案：进行中 → 剩 3天02:13:45；即将开始 → 02:13:45后开抢 */
function buildRemainText(status, now) {
    if (status === 'ongoing' && activityEnd) {
        return ' · 剩 ' + formatDuration(activityEnd.getTime() - now);
    }
    if (status === 'upcoming' && activityStart) {
        return ' · ' + formatDuration(activityStart.getTime() - now) + '后开抢';
    }
    return '';
}

function renderBenefitText() {
    const parts = [];
    if (benefitSnapshot.deductionPrice > 0) {
        parts.push('直降 ¥' + formatPrice(benefitSnapshot.deductionPrice));
    }
    parts.push(benefitSnapshot.targetCount + '人拼团');
    if (currentActivityState === 'ended') {
        parts.push('活动已结束');
    } else if (currentActivityState === 'upcoming') {
        parts.push('活动即将开始');
    } else {
        parts.push('参与马上拼到');
    }
    setText('benefitTag', benefitSnapshot.deductionPrice > 0 ? '大额优惠' : '拼团优惠');
    setText('benefitText', parts.join('，'));
}

/** 活动不在进行中时置灰按钮，并写清原因 */
function applyActivityGate() {
    const open = currentActivityState === '' || currentActivityState === 'ongoing';
    setBuyEnabled(open);

    const reasonText = {
        ended: '活动已结束，暂时无法下单',
        upcoming: '活动还未开始，到点后即可下单'
    }[currentActivityState] || '';

    const hint = document.getElementById('buyHint');
    if (hint) {
        hint.hidden = !reasonText;
        hint.innerText = reasonText;
    }
    ['singleBuyBtn', 'groupBuyBtn'].forEach(function (id) {
        const button = document.getElementById(id);
        if (button) button.title = reasonText;
    });
}

function setBuyEnabled(enabled) {
    ['singleBuyBtn', 'groupBuyBtn'].forEach(function (id) {
        const button = document.getElementById(id);
        if (button) button.disabled = !enabled;
    });

    const list = document.getElementById('memberList');
    if (list) {
        list.querySelectorAll('.grab-btn').forEach(function (button) {
            button.disabled = enabled ? button.dataset.locked === '1' : true;
        });
    }
}

/* ---------------------- 正在拼的团 ---------------------- */
function renderTeams(teamList) {
    const list = document.getElementById('memberList');
    if (!list) return;

    teamTimers.forEach(function (timer) { clearInterval(timer); });
    teamTimers.length = 0;

    // 只保留「还在拼」的团：已成团、已结束的一律不展示
    const teams = mergeTeams(teamList).filter(isTeamInProgress);
    if (!teams.length) {
        list.innerHTML = TEAM_EMPTY_HTML;
        return;
    }

    list.innerHTML = teams.map(function (team, index) {
        const target = toNumber(team.targetCount) || 2;
        const members = teamMembers(team);
        const joined = teamJoinedCount(team);
        const lack = Math.max(0, target - joined);
        const progress = target > 0 ? Math.min(100, Math.round((joined / target) * 100)) : 0;
        const view = teamRowView(team);
        const leader = (members && members[0] && members[0].userId) || team.userId || '匿名用户';

        return '<div class="member-item' + (view.mine ? ' mine' : '') + '">' +
            '<div class="member-avatar">' + escapeHtml(String(leader).substring(0, 1).toUpperCase()) + '</div>' +
            '<div class="member-info">' +
                // data-teamid 挂在 .user-info 上：下单时按团号参团
                '<div class="user-info" data-teamid="' + escapeHtml(team.teamId || '') + '"' +
                    ' data-activityid="' + escapeHtml(team.activityId || currentActivityId) + '">' +
                    '<div class="member-name">' +
                        escapeHtml(obfuscateUserId(leader)) +
                        (view.badge ? '<span class="mine-badge ' + view.badgeClass + '">' + view.badge + '</span>' : '') +
                    '</div>' +
                    '<div class="member-desc">' +
                        '<span class="settle-tag">团号 ' + escapeHtml(team.teamId || '待成团') + '</span>' +
                        '<span class="lack-text">还差 ' + lack + ' 人</span>' +
                    '</div>' +
                '</div>' +
                renderMemberStrip(members, target) +
                '<div class="team-progress"><i style="width:' + progress + '%"></i></div>' +
                '<div class="team-meta">' + joined + '/' + target +
                    ' 人已参与 · 结束 ' + escapeHtml(formatDateTime(team.validEndTime)) + '</div>' +
            '</div>' +
            '<div class="member-action">' +
                '<button class="btn btn-primary btn-small btn-pill group-btn grab-btn" data-index="' + index + '"' +
                    (view.actionable ? '' : ' disabled data-locked="1"') + '>' + view.buttonText + '</button>' +
                '<span class="member-time" id="teamTime' + index + '">' +
                    escapeHtml(team.validTimeCountdown || '--:--:--') + '</span>' +
            '</div>' +
        '</div>';
    }).join('');

    teams.forEach(function (team, index) {
        startTeamCountdown(index, team);
    });

    // 点「立即抢单 / 继续支付」→ 带该团 teamId 去 s-pay-mall 下单
    list.onclick = function (event) {
        const button = event.target.closest('.grab-btn');
        if (!button || button.disabled) return;
        const team = teams[Number(button.dataset.index)];
        if (!team) return;

        // teamId 从这一行的 .user-info[data-teamid] 取（与原前端约定一致）
        const item = button.closest('.member-item');
        const userInfo = item ? item.querySelector('.user-info') : null;
        const teamId = (userInfo && userInfo.dataset.teamid) || team.teamId || '';
        doOrder(teamId, true);
    };

    applyActivityGate();
}

/** 同一个团可能返回多行（我的那条 + 随机推荐），合并成一行 */
function mergeTeams(teamList) {
    const me = currentUserId();
    const map = new Map();

    (teamList || []).forEach(function (team) {
        if (!team || !team.teamId) return;
        const mine = String(team.userId || '') === String(me);
        const exists = map.get(team.teamId);
        if (!exists) {
            map.set(team.teamId, Object.assign({}, team, { __mine: mine }));
            return;
        }
        // 同一个团多行时保留"我自己"那行 —— 只有它的状态才是我的订单状态
        if (mine && !exists.__mine) {
            map.set(team.teamId, Object.assign({}, team, { __mine: true }));
        }
    });

    return Array.from(map.values()).sort(function (a, b) {
        return (b.__mine ? 1 : 0) - (a.__mine ? 1 : 0);
    });
}

/** 「正在拼的团」列表为空时的提示 */
const TEAM_EMPTY_HTML = '<div class="team-empty">暂时没有进行中的拼团，点下面「开团购买」当团长吧～</div>';

/** 这个团实际的已参与人数：有成员明细以明细为准，否则用锁单/完成数兜底 */
function teamJoinedCount(team) {
    const members = teamMembers(team);
    return members ? members.length : Math.max(toNumber(team.lockCount), toNumber(team.completeCount));
}

/** 团是否已经结束：优先用 validEndTime，没有则认接口给的「已结束」倒计时 */
function isTeamExpired(team) {
    const endDate = parseDate(team && team.validEndTime);
    if (endDate) return endDate.getTime() <= Date.now();
    return String((team && team.validTimeCountdown) || '').trim() === '已结束';
}

/**
 * 这个团还算不算「正在拼」：没成团 + 没过期。
 * 后端只该返回可参团的团，这里再兜一层，挡住两类脏数据：
 *   1. 已成团 —— 已参与人数达到目标人数（成团后 status 变 1，但满员未结算的团仍是 0）
 *   2. 已结束 —— 拼团有效时间已经过了（后端只挡了活动 end_time，没挡团的 valid_end_time）
 * 页面停留期间团也会自然过期，靠 removeTeamRow 在倒计时归零时移除。
 */
function isTeamInProgress(team) {
    const target = toNumber(team && team.targetCount);
    if (target > 0 && teamJoinedCount(team) >= target) return false;
    if (isTeamExpired(team)) return false;
    return true;
}

/* 拼团订单明细状态：0 创建（待支付）/ 1 完成（已结算）/ 2 关闭 */
const ORDER_STATUS = { CREATE: 0, COMPLETE: 1, CLOSE: 2 };

/**
 * 这一行怎么展示：团属不属于我 + 我的订单状态
 *   别人的团 → 立即抢单（可以参团）
 *   我的团 · 待支付 → 继续支付（再点一次会用 s-pay-mall 里那张未支付单，不会重复锁单）
 *   我的团 · 已参与/已关闭 → 不可点
 */
function teamRowView(team) {
    const me = currentUserId();
    const members = teamMembers(team);
    const member = members ? members.find(function (item) {
        return String(item.userId || '') === String(me);
    }) : null;

    const mine = members ? !!member
        : ((typeof team.__mine === 'boolean') ? team.__mine : String(team.userId || '') === String(me));
    const status = members ? (member ? toOrderStatus(member.status) : null) : toOrderStatus(team.status);

    if (!mine) {
        return { mine: false, status: status, badge: '', badgeClass: '', buttonText: '立即抢单', actionable: true };
    }
    if (status === ORDER_STATUS.CREATE) {
        return { mine: true, status: status, badge: '我的团 · 待支付', badgeClass: 'pending', buttonText: '继续支付', actionable: true };
    }
    if (status === ORDER_STATUS.COMPLETE) {
        return { mine: true, status: status, badge: '我的团 · 已参与', badgeClass: 'done', buttonText: '已参与', actionable: false };
    }
    if (status === ORDER_STATUS.CLOSE) {
        return { mine: true, status: status, badge: '我的团 · 已关闭', badgeClass: 'closed', buttonText: '已关闭', actionable: false };
    }
    return { mine: true, status: status, badge: '我的团', badgeClass: '', buttonText: '已加入', actionable: false };
}

/** 接口没返回成员列表（老版本）时返回 null */
function teamMembers(team) {
    const list = team && team.memberList;
    return (Array.isArray(list) && list.length) ? list : null;
}

/** 成员头像条：每个成员一个圆点，我的高亮，最多显示 5 个 */
function renderMemberStrip(members, target) {
    if (!members) return '';
    const me = currentUserId();
    const shown = members.slice(0, 5).map(function (item) {
        const isMe = String(item.userId || '') === String(me);
        const settled = toOrderStatus(item.status) === ORDER_STATUS.COMPLETE;
        return '<span class="member-chip' + (isMe ? ' me' : '') + (settled ? ' settled' : '') +
            '" title="' + escapeHtml(item.userId || '') + (settled ? '（已支付）' : '（待支付）') + '">' +
            escapeHtml(String(item.userId || '?').substring(0, 1).toUpperCase()) + '</span>';
    }).join('');
    const more = members.length > 5 ? '<span class="member-chip more">+' + (members.length - 5) + '</span>' : '';
    return '<div class="member-strip">' + shown + more + '</div>';
}

function toOrderStatus(value) {
    if (value === null || value === undefined || value === '') return null;
    const num = Number(value);
    return isNaN(num) ? null : num;
}

/** 每个团一个本地倒计时：优先用 validEndTime，接口给的 validTimeCountdown 兜底 */
function startTeamCountdown(index, team) {
    const timeEl = document.getElementById('teamTime' + index);
    if (!timeEl) return;

    let seconds = remainingSeconds(team);

    // 已经过期（或接口明确说已结束）：这个团不再是「正在拼的团」，直接不展示
    if (seconds <= 0 && isTeamExpired(team)) {
        removeTeamRow(timeEl);
        return;
    }

    const timer = setInterval(function () {
        seconds--;
        if (seconds <= 0) {
            clearInterval(timer);
            // 页面停留期间团自然过期 → 把这一行从「正在拼的团」移除
            removeTeamRow(timeEl);
            return;
        }
        const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
        const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
        const s = String(seconds % 60).padStart(2, '0');
        timeEl.innerText = h + ':' + m + ':' + s;
        if (seconds <= 60) timeEl.classList.add('ending');
    }, 1000);

    teamTimers.push(timer);
}

/** 团已过期：把这一行从「正在拼的团」里移除，列表空了就回到空态提示 */
function removeTeamRow(timeEl) {
    const item = timeEl && timeEl.closest ? timeEl.closest('.member-item') : null;
    if (item && item.parentNode) {
        item.parentNode.removeChild(item);
    }
    refreshTeamEmptyState();
}

function refreshTeamEmptyState() {
    const list = document.getElementById('memberList');
    if (!list) return;
    if (list.querySelector('.member-item')) return;
    list.innerHTML = TEAM_EMPTY_HTML;
}

function remainingSeconds(team) {
    const endDate = parseDate(team.validEndTime);
    if (endDate) {
        const diff = Math.floor((endDate.getTime() - Date.now()) / 1000);
        return diff > 0 ? diff : 0;
    }
    const matched = /^(\d{1,2}):(\d{2}):(\d{2})$/.exec(team.validTimeCountdown || '');
    if (matched) {
        return Number(matched[1]) * 3600 + Number(matched[2]) * 60 + Number(matched[3]);
    }
    return 0;
}

/* ---------------------- 拼团统计 ---------------------- */
function renderStatistic(statistic, teamList) {
    const box = document.getElementById('teamStatistic');
    if (!box) return;

    const allTeamCount = toNumber(statistic && statistic.allTeamCount);
    const completeCount = toNumber(statistic && statistic.getTeamCompleteCount);
    const userCount = toNumber(statistic && statistic.allTeamUserCount);

    if (allTeamCount === 0 && teamList && teamList.length) {
        const people = teamList.reduce(function (sum, team) {
            return sum + Math.max(toNumber(team.lockCount), toNumber(team.completeCount));
        }, 0);
        box.innerHTML = '已有 <strong>' + people + '</strong> 人参与拼团';
        return;
    }
    box.innerHTML = '已开团 <strong>' + allTeamCount + '</strong> 个 · 已成团 <strong>' +
        completeCount + '</strong> 个 · <strong>' + userCount + '</strong> 人参与';
}

/* ---------------------- 小工具 ---------------------- */
function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.innerText = text;
}

/* =====================================================================
 * 公共脚本：登录态 + 请求封装 + 小工具
 *   —— 登录态沿用 s-pay-mall 的 cookie：loginToken（login.html 扫码登录成功后写入）
 *   —— 两套后端地址：
 *        sPayMallUrl       小型支付商城（本项目，dev 端口 8000）：下单 / 支付
 *        groupBuyMarketUrl 拼团营销系统（端口 8091）：只查询拼团信息
 *   —— 地址可以在地址栏临时覆盖，方便换端口联调：
 *        index.html?pay=http://127.0.0.1:8000&market=http://127.0.0.1:8091
 * ===================================================================== */

const _qs = new URLSearchParams(location.search);
const trimSlash = (s) => String(s || '').replace(/\/+$/, '');

/** 小型支付商城地址（下单、支付都走这里） */
// ★ 本地调试版：静态页面跑在别的端口（如 8899），接口指向本机服务
const sPayMallUrl = trimSlash(_qs.get('pay') || 'http://127.0.0.1:8000');
/** 拼团营销系统地址（只用于查询拼团信息） */
const groupBuyMarketUrl = trimSlash(_qs.get('market') || 'http://127.0.0.1:8091');

/** 与拼团系统 sku 表一致的渠道/商品参数 */
const MARKET_SOURCE = 's01';
const MARKET_CHANNEL = 'c01';
const GOODS_ID = '9890001';

/** 营销类型：0 无营销（单独购买，按原价）；1 营销拼团 */
const MARKET_TYPE = { NO_MARKET: 0, GROUP_BUY_MARKET: 1 };

const COOKIE_USERNAME = 'loginToken';
const COOKIE_EXPIRE_DAYS = 30;

/* ------------------------- Cookie ------------------------- */
function setCookie(name, value, days) {
    const expireDays = (typeof days === 'number') ? days : COOKIE_EXPIRE_DAYS;
    const expires = new Date(Date.now() + expireDays * 24 * 60 * 60 * 1000).toUTCString();
    document.cookie = name + '=' + encodeURIComponent(value) + ';expires=' + expires + ';path=/;SameSite=Lax';
}

function getCookie(name) {
    const key = name + '=';
    const cookies = document.cookie ? document.cookie.split(';') : [];
    for (let i = 0; i < cookies.length; i++) {
        const item = cookies[i].trim();
        if (item.indexOf(key) === 0) return decodeURIComponent(item.substring(key.length));
    }
    return '';
}

function removeCookie(name) {
    document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';
}

/** 当前登录用户（openid），未登录返回空串 */
function currentUserId() {
    return getCookie(COOKIE_USERNAME);
}

/** 页面守卫：未登录跳登录页 */
function requireLogin() {
    const userId = currentUserId();
    if (!userId) location.replace('login.html');
    return userId;
}

function logout() {
    removeCookie(COOKIE_USERNAME);
    location.replace('login.html');
}

/* ------------------------- 请求 ------------------------- */
/** 统一的 POST JSON；非 2xx 直接抛错，方便调用方统一 toast */
async function postJson(url, payload) {
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload || {})
    });
    if (!response.ok) throw new Error('HTTP ' + response.status + ' ← ' + url);
    return response.json();
}

/**
 * 查询拼团营销配置（走拼团系统）
 * POST {groupBuyMarketUrl}/api/v1/gbm/index/query_group_buy_market_config
 */
function queryMarketConfig() {
    return postJson(groupBuyMarketUrl + '/api/v1/gbm/index/query_group_buy_market_config', {
        userId: currentUserId(),
        source: MARKET_SOURCE,
        channel: MARKET_CHANNEL,
        goodsId: GOODS_ID
    });
}

/**
 * 下单（走 s-pay-mall）
 * POST {sPayMallUrl}/api/v1/alipay/create_pay_order
 * @param {object} body { userId, productId, teamId?, activityId?, marketType }
 * 返回 data：{ orderId, payUrl, marketType, payAmount, marketDeductionAmount, marketFallback }
 *   marketFallback = true 表示"请求拼团但优惠没生效"（例如拼团名额已用完），已按原价成单
 */
function createPayOrder(body) {
    return postJson(sPayMallUrl + '/api/v1/alipay/create_pay_order', body);
}

/**
 * 查询我的订单列表（游标分页，走 s-pay-mall）
 * POST {sPayMallUrl}/api/v1/alipay/query_user_order_list
 * @param {string} userId 当前登录用户
 * @param {number|null} lastId 上一页返回的 lastId；第一页传 null
 * @param {number} pageSize 每页条数（默认 10）
 * 返回 data：{ orderList:[{ id, userId, productId, productName, orderId, orderTime,
 *              totalAmount, status, payUrl, marketType, marketDeductionAmount,
 *              payAmount, payTime }], hasMore, lastId }
 *   status 取值：CREATE / PAY_WAIT / PAY_SUCCESS / DEAL_DONE / CLOSE
 */
function queryUserOrderList(userId, lastId, pageSize) {
    return postJson(sPayMallUrl + '/api/v1/alipay/query_user_order_list', {
        userId: userId,
        lastId: (lastId === undefined ? null : lastId),
        pageSize: pageSize || 10
    });
}

/**
 * 退单（走 s-pay-mall）
 * POST {sPayMallUrl}/api/v1/alipay/refund_order
 * 返回 data：{ success, message, orderId }
 *   注意：业务失败时 code 仍是 0000，需再看 data.success
 */
function refundOrder(userId, orderId) {
    return postJson(sPayMallUrl + '/api/v1/alipay/refund_order', {
        userId: userId,
        orderId: orderId
    });
}

/* ------------------------- 小工具 ------------------------- */
function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function toNumber(value) {
    const num = Number(value);
    return isNaN(num) ? 0 : num;
}

function formatPrice(value) {
    const num = toNumber(value);
    return Number.isInteger(num) ? String(num) : num.toFixed(2);
}

/** 兼容 ISO 字符串 / 时间戳 / Date 三种时间格式，解析失败返回 null */
function parseDate(value) {
    if (value === null || value === undefined || value === '') return null;
    const date = (value instanceof Date) ? value
        : (typeof value === 'number' || /^\d+$/.test(String(value))) ? new Date(Number(value))
            : new Date(value);
    return isNaN(date.getTime()) ? null : date;
}

function formatDateTime(value) {
    const date = parseDate(value);
    if (!date) return '-';
    const pad = (n) => String(n).padStart(2, '0');
    return pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' +
        pad(date.getHours()) + ':' + pad(date.getMinutes());
}

/** 倒计时格式：1天以上带天数，否则纯 HH:mm:ss */
function formatDuration(milliseconds) {
    if (milliseconds <= 0) return '00:00:00';
    const totalSeconds = Math.floor(milliseconds / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const h = String(Math.floor((totalSeconds % 86400) / 3600)).padStart(2, '0');
    const m = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0');
    const s = String(totalSeconds % 60).padStart(2, '0');
    const clock = h + ':' + m + ':' + s;
    return days > 0 ? days + '天' + clock : clock;
}

/** 用户ID打码：只留前两位后两位（展示团长用） */
function obfuscateUserId(userId) {
    const text = String(userId || '');
    if (text.length <= 4) return text || '匿名用户';
    return text.slice(0, 2) + '*'.repeat(text.length - 4) + text.slice(-2);
}

/* ------------------------- 轻提示 ------------------------- */
function copyText(text) {
    if (!navigator.clipboard) return;
    navigator.clipboard.writeText(String(text))
        .then(function () { showToast('已复制：' + text, 'success'); })
        .catch(function () {});
}

function showToast(message, type) {
    let el = document.getElementById('sPayToast');
    if (!el) {
        el = document.createElement('div');
        el.id = 'sPayToast';
        document.body.appendChild(el);
    }
    el.className = 'gbm-toast ' + (type || 'info');
    el.innerText = message;
    void el.offsetWidth;             // 强制回流，保证连续调用也有动画
    el.classList.add('show');

    clearTimeout(el.__timer);
    if (type !== 'info') {
        el.__timer = setTimeout(() => el.classList.remove('show'), 3000);
    }
}

function hideToast() {
    const el = document.getElementById('sPayToast');
    if (el) {
        clearTimeout(el.__timer);
        el.classList.remove('show');
    }
}

console.info('[s-pay-mall] 支付商城：' + sPayMallUrl + '，拼团系统：' + groupBuyMarketUrl);
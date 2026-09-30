/* =====================================================================
 * 支付确认弹窗（s-pay-mall）
 *   结构沿用 group-buy-market 的弹窗样式（css/modal.css），内容改成支付宝沙箱支付：
 *     - 显示本单实付金额（以后端返回的 payAmount 为准）
 *     - 拼团优惠没生效（marketFallback = true，如名额已用完）时，明确提示"按原价支付"
 *     - 点确认 → 提交 s-pay-mall 返回的支付宝表单，跳转沙箱收银台
 * ===================================================================== */

(function () {
    let insertedFormHtml = null;

    /** 打开支付确认弹窗 */
    window.openPayModal = function (opts) {
        const o = opts || {};
        const amount = toNumber(o.amount);
        const fallback = o.marketFallback === true;
        const formHtml = o.formHtml || '';
        insertedFormHtml = formHtml;

        window.closePayModal();

        // 支付宝表单先插进页面（它是隐藏的），点确认时直接 submit
        if (formHtml) document.body.insertAdjacentHTML('beforeend', formHtml);

        const orderInfo = [];
        if (o.orderId) orderInfo.push('订单号 ' + o.orderId);
        orderInfo.push(o.teamId ? '团号 ' + o.teamId : '待成团');

        const okText = fallback ? '按原价去支付' : '去支付宝支付';
        const warnHtml = fallback
            ? '<div class="pay-warn">' +
              '<strong>拼团名额已用完</strong>' +
              '本单无法享受拼团优惠，需要按原价 ¥' + escapeHtml(formatPrice(amount)) + ' 支付' +
              '</div>'
            : '';

        const overlay = document.createElement('div');
        overlay.id = 'modalOverlay';
        overlay.className = 'modal-overlay';
        overlay.innerHTML =
            '<div class="modal-content">' +
                '<button class="modal-close" id="payModalClose">×</button>' +
                '<div class="modal-title">' + (fallback ? '按原价支付' : '确认支付') + '</div>' +
                '<div class="modal-amount">实付金额 <strong>¥ ' + escapeHtml(formatPrice(amount)) + '</strong></div>' +
                '<div class="modal-order">' + escapeHtml(orderInfo.join(' · ')) + '</div>' +
                warnHtml +
                '<div class="pay-account">' +
                    '<strong>沙箱测试买家账号</strong>' +
                    '账号：<span class="copyable" data-copy="kvhmoj3832@sandbox.com">kvhmoj3832@sandbox.com</span>（点击复制）<br>' +
                    '登录密码：111111　支付密码：111111' +
                '</div>' +
                '<p class="qr-tip">点击「' + okText + '」将跳转到支付宝沙箱收银台</p>' +
                '<div class="modal-footer">' +
                    '<button class="btn btn-ghost" id="payCancelBtn">取消</button>' +
                    '<button class="btn btn-primary" id="payOkBtn">' + okText + '</button>' +
                '</div>' +
            '</div>';
        document.body.appendChild(overlay);

        // 点遮罩关闭
        overlay.addEventListener('click', function (event) {
            if (event.target === overlay) cancelPay();
        });
        document.getElementById('payModalClose').onclick = cancelPay;
        document.getElementById('payCancelBtn').onclick = cancelPay;
        document.getElementById('payOkBtn').onclick = confirmPay;

        const copyEl = overlay.querySelector('.copyable');
        if (copyEl) {
            copyEl.onclick = function () {
                if (navigator.clipboard) {
                    navigator.clipboard.writeText(copyEl.dataset.copy)
                        .then(() => showToast('买家账号已复制', 'success'))
                        .catch(() => {});
                }
            };
        }
    };

    /** 关闭弹窗，并清掉注入的支付宝表单 */
    window.closePayModal = function () {
        const overlay = document.getElementById('modalOverlay');
        if (overlay) overlay.remove();
        document.querySelectorAll('form').forEach(function (form) {
            form.remove();
        });
        insertedFormHtml = null;
    };

    /** 取消支付 */
    function cancelPay() {
        closePayModal();
        if (typeof window.onPayCancel === 'function') window.onPayCancel();
    }

    /** 确认支付：提交支付宝表单跳收银台 */
    function confirmPay() {
        const form = document.forms['punchout_form'] || document.querySelector('form');
        if (!form) {
            closePayModal();
            showToast('支付表单创建失败，请重新下单', 'error');
            return;
        }
        form.submit();
    }

    window.cancelPay = cancelPay;
    window.confirmPay = confirmPay;
})();

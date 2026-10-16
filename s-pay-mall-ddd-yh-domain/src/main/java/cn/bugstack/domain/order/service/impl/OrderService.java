package cn.bugstack.domain.order.service.impl;

import cn.bugstack.domain.order.adapter.port.IProductPort;
import cn.bugstack.domain.order.adapter.repository.IAnalyticsRepository;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.MarketPayDiscountEntity;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 18:06
 */

@Service
@Slf4j
public class OrderService extends AbstractOrderService{

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private IAnalyticsRepository analyticsRepository;


    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    public OrderService(IOrderRepository repository, IProductPort port){
        super(repository,port);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException {
        return doPrepayOrder(userId, productId, productName, orderId, totalAmount, null);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount, MarketPayDiscountEntity marketPayDiscountEntity) throws AlipayApiException {
        // 支付金额
        BigDecimal payAmount = null == marketPayDiscountEntity ? totalAmount : marketPayDiscountEntity.getPayPrice();

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);
        bizContent.put("total_amount", payAmount);
        bizContent.put("subject", productName);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        String form = alipayClient.pageExecute(request).getBody();

        // 表单生成失败（为空或不是 HTML 表单）时不要落库：坏表单存进 pay_url 后，
        // 用户点支付会被支付宝收银台直接拒绝（例如返回 {"resultCode":"USER_NOT_LOGIN", ...} 这类信封）
        if (null == form || !form.contains("<form")) {
            log.error("支付宝支付表单生成失败 userId:{} orderId:{} form:{}", userId, orderId, form);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "支付宝支付表单生成失败");
        }

        PayOrderEntity payOrderEntity = new PayOrderEntity();
        payOrderEntity.setOrderId(orderId);
        payOrderEntity.setPayUrl(form);
        payOrderEntity.setOrderStatusVO(OrderStatusVO.PAY_WAIT);
        payOrderEntity.setMarketType(marketPayDiscountEntity == null ? MarketTypeVO.NO_MARKET.getCode() : MarketTypeVO.GROUP_BUY_MARKET.getCode());
        payOrderEntity.setPayAmount(payAmount);
        payOrderEntity.setMarketDeductionAmount(marketPayDiscountEntity == null ? BigDecimal.ZERO : marketPayDiscountEntity.getDeductionPrice());
        orderRepository.updateOrderPayInfo(payOrderEntity);
        return payOrderEntity;
    }

    @Override
    protected MarketPayDiscountEntity lockMarketPayOrder(String userId, String teamId, Long activityId, String productId, String orderId) {
        return productPort.lockMarketPayOrder(userId,  teamId,  activityId,  productId,  orderId);
    }


    @Override
    protected void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        orderRepository.doSaveOrder(createOrderAggregate);
    }

    @Override
    public void changeOrderPaySuccess(String orderId, Date payTime) {

        OrderEntity orderEntity = orderRepository.queryOrderByOrderId(orderId);
        if (orderEntity == null){
            return;
        }
        if(MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(orderEntity.getMarketType())){
            orderRepository.changeMarketOrderPaySuccess(orderId);
            // 发起营销结算。这个过程可以是http/rpc直接调用，也可以发一个商城交易支付完成的消息，之后拼团系统自己接收做结算。
            try {
                productPort.settlementMarketPayOrder(orderEntity.getUserId(), orderId, payTime);
            } catch (AppException e) {
                // 业务性拒绝：渠道黑名单(E0105) / 支付超期(E0106) —— 拼团永远不会结算/成团这笔单。
                // 用户已经付了钱，必须自动退款并关单，否则会卡在「已支付未结算」。
                if ("E0105".equals(e.getCode()) || "E0106".equals(e.getCode())) {
                    log.error("拼团结算被拒，启动自动退款 orderId:{} code:{} msg:{}", orderId, e.getCode(), e.getInfo());
                    try {
                        refundPayOrder(orderEntity.getUserId(), orderId);
                    } catch (Exception refundEx) {
                        log.error("自动退款失败，标记待退款，交给退款补偿任务重试 orderId:{}", orderId, refundEx);
                        orderRepository.changeOrderWaitRefund(orderEntity.getUserId(), orderId);
                    }
                    return;   // 已退款关单，不再置为支付成功
                }
                // 其它异常（网络等）：标记待结算，交给补偿任务重试
                log.warn("拼团结算失败（非业务性拒绝），标记待结算 orderId:{}", orderId, e);
                orderRepository.changeMarketOrderWaitSettlement(orderId);
                return;   // 标记待结算后提前返回，避免被后面的 changeOrderPaySuccess 覆盖回 PAY_SUCCESS
            } catch (Exception e) {
                // 其它异常（网络等）：标记待结算，交给补偿任务重试
                log.warn("拼团结算异常，标记待结算 orderId:{}", orderId, e);
                orderRepository.changeMarketOrderWaitSettlement(orderId);
                return;   // 标记待结算后提前返回，交给 SettlementRetryJob 重试
            }

        }

        orderRepository.changeOrderPaySuccess(orderId,payTime);
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderRepository.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderRepository.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderRepository.changeOrderClose(orderId);
    }

    @Override
    public void changeOrderMarketSettlement(List<String> outTradeNoList) {
        orderRepository.changeOrderMarketSettlement(outTradeNoList);
    }


    @Override
    public boolean refundOrder(String userId, String orderId) {
        // 1. 查询订单信息，验证订单是否存在且属于该用户
        OrderEntity orderEntity = orderRepository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (null == orderEntity) {
            log.warn("退单失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, orderId);
            return false;
        }
        // 2. 检查订单状态，只有create、pay_wait、pay_success、deal_done状态的订单可以退单
        String status = orderEntity.getOrderStatusVO().getCode();
        if (OrderStatusVO.CLOSE.getCode().equals(status)) {
            log.warn("退单失败，订单已关闭 userId:{} orderId:{} status:{}", userId, orderId, status);
            return false;
        }
        // 3. 对于营销类型的单子，调用拼团执行组队退单 todo xfg

        // 4. 执行退单操作
        boolean result = orderRepository.refundOrder(userId, orderId);
        if (result) {
            log.info("退单成功 userId:{} orderId:{}", userId, orderId);
        } else {
            log.warn("退单失败 userId:{} orderId:{}", userId, orderId);
        }
        return result;
    }


    @Override
    public boolean refundMarketOrder(String userId, String orderId) {
        // 1. 查询订单信息，验证订单是否存在且属于该用户
        OrderEntity orderEntity = orderRepository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (null == orderEntity) {
            log.warn("退单失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, orderId);
            return false;
        }

        // 2. 检查订单状态，只有create、pay_wait、pay_success、deal_done状态的订单可以退单
        String status = orderEntity.getOrderStatusVO().getCode();
        if (OrderStatusVO.CLOSE.getCode().equals(status)) {
            log.warn("退单失败，订单已关闭 userId:{} orderId:{} status:{}", userId, orderId, status);
            return false;
        }

        // 3. 对于营销类型的单子，调用拼团执行组队退单
        productPort.refundMarketPayOrder(userId, orderId);
        if (null == orderEntity.getPayTime()) {                      // ② 没付款：不存在退款，直接关单
            return orderRepository.refundOrder(userId, orderId);     //    → CLOSE
        }

            boolean result = orderRepository.refundMarketOrder(userId, orderId);
            if (result) {
                log.info("退单成功 userId:{} orderId:{}", userId, orderId);
            } else {
                log.warn("退单失败 userId:{} orderId:{}", userId, orderId);
            }
            return result;

    }

    @Override
    public boolean refundPayOrder(String userId, String orderId) throws AlipayApiException {
        // 1. 查询订单信息，验证订单是否存在且属于该用户
        OrderEntity orderEntity = orderRepository.queryOrderByUserIdAndOrderId(userId, orderId);
        if (null == orderEntity) {
            log.warn("退款失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, orderId);
            return false;
        }

        // 2. 幂等：已关闭的订单直接返回成功，避免重复退款
        if (OrderStatusVO.CLOSE.getCode().equals(orderEntity.getOrderStatusVO().getCode())) {
            log.info("订单已关闭，退款幂等返回 userId:{} orderId:{}", userId, orderId);
            return true;
        }

        // 3. 没收到过钱就没有交易可退：未支付订单直接关单，绝不能调支付宝退款，
        //    否则必然返回 40004 / ACQ.TRADE_NOT_EXIST（交易不存在）
        if (null == orderEntity.getPayTime()) {
            log.info("订单未支付，无需支付宝退款，直接关单 userId:{} orderId:{}", userId, orderId);
            return orderRepository.refundOrder(userId, orderId);   // status = CLOSE
        }

        // 4. 已支付：调用支付宝退款
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();
        refundModel.setOutTradeNo(orderEntity.getOrderId());
        refundModel.setRefundAmount(orderEntity.getPayAmount().toString());
        refundModel.setRefundReason("交易退单");
        request.setBizModel(refundModel);

        AlipayTradeRefundResponse execute = alipayClient.execute(request);
        if (!execute.isSuccess()) {
            // 交易不存在(ACQ.TRADE_NOT_EXIST)、已全额退款 等属于业务性失败，重试也不会成功，需人工核对或对账
            log.error("支付宝退款失败 userId:{} orderId:{} code:{} subCode:{} subMsg:{}",
                    userId, orderId, execute.getCode(), execute.getSubCode(), execute.getSubMsg());
            return false;
        }
        // 状态变更
        orderRepository.refundOrder(userId, orderId);
        log.info("支付宝退款成功 userId:{} orderId:{}", userId, orderId);

        return true;
    }

    @Override
    public OrderEntity queryOrder(String userId, String orderId) {
        return orderRepository.queryOrderByUserIdAndOrderId(userId, orderId);
    }

    @Override
    public void syncOrdersToAnalytics() {
        Long lastOrderId = analyticsRepository.getSyncCheckpoint();
        if (null == lastOrderId) lastOrderId = 0L;
        int batch = 500;
        List<OrderEntity> orders;
        do {
            orders = orderRepository.queryIncrementalOrders(lastOrderId, batch);
            if (null == orders || orders.isEmpty()) break;
            for (OrderEntity order : orders) {
                analyticsRepository.upsertFactOrder(order);
                lastOrderId = order.getId();
            }
            analyticsRepository.updateSyncCheckpoint(lastOrderId);
        } while (orders.size() == batch);
        analyticsRepository.recomputeUserSummary();
        log.info("商城订单同步分析库完成 lastOrderId:{}", lastOrderId);
    }

    @Override
    public List<OrderEntity> queryWaitSettlementOrderList() {
        return orderRepository.queryWaitSettlementOrderList();
    }

    @Override
    public List<OrderEntity> queryWaitRefundOrderList() {
        return orderRepository.queryWaitRefundOrderList();
    }

    @Override
    public void closeRefundOrder(String userId, String outTradeNo) {
        // 1. 查询订单信息，验证订单是否存在且属于该用户
        OrderEntity orderEntity = orderRepository.queryOrderByUserIdAndOrderId(userId, outTradeNo);
        if (null == orderEntity) {
            log.warn("关闭退单失败，订单不存在或不属于该用户 userId:{} orderId:{}", userId, outTradeNo);
            return;
        }
        // 2. 检查订单状态，只有create、pay_wait、pay_success、deal_done状态的订单可以关闭退单
        String status = orderEntity.getOrderStatusVO().getCode();
        if (OrderStatusVO.CLOSE.getCode().equals(status)) {
            log.warn("关闭退单失败，订单已关闭 userId:{} orderId:{} status:{}", userId, outTradeNo, status);
            return;
        }
        // 3. 执行关闭退单操作
        boolean result = orderRepository.closeRefundOrder(userId, outTradeNo);
        if (result) {
            log.info("关闭退单成功 userId:{} orderId:{}", userId, outTradeNo);
        } else {
            log.warn("关闭退单失败 userId:{} orderId:{}", userId, outTradeNo);
        }
    }
}

package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ProductEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.infrastructure.dao.IOrderDao;
import cn.bugstack.infrastructure.dao.po.PayOrder;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.types.event.BaseEvent;
import com.alibaba.fastjson.JSON;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 20:37
 */

@Service
@Slf4j
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;

    @Resource
    private EventBus eventBus;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private PaySuccessMessageEvent paySuccessMessageEvent;

    @Override
    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity) {

        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setUserId(shopCartEntity.getUserId());
        payOrderReq.setProductId(shopCartEntity.getProductId());

        PayOrder unpaidOrder = orderDao.queryUnPayOrder(payOrderReq);

        if (unpaidOrder == null){ return null;}

        return OrderEntity.builder()
                .productId(unpaidOrder.getProductId())
                .productName(unpaidOrder.getProductName())
                .orderId(unpaidOrder.getOrderId())
                .orderStatusVO(OrderStatusVO.valueOf(unpaidOrder.getStatus()))
                .orderTime(unpaidOrder.getOrderTime())
                .totalAmount(unpaidOrder.getTotalAmount())
                .payUrl(unpaidOrder.getPayUrl())
                .marketType(unpaidOrder.getMarketType())
                .marketDeductionAmount(unpaidOrder.getMarketDeductionAmount())
                .payAmount(unpaidOrder.getPayAmount())
                .build();
    }

    @Override
    public void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        OrderEntity orderEntity = createOrderAggregate.getOrderEntity();
        ProductEntity productEntity = createOrderAggregate.getProductEntity();
        String userId = createOrderAggregate.getUserId();
        PayOrder payOrder = new PayOrder();
        payOrder.setUserId(userId);
        payOrder.setProductId(productEntity.getProductId());
        payOrder.setProductName(productEntity.getProductName());
        payOrder.setOrderId(orderEntity.getOrderId());
        payOrder.setOrderTime(orderEntity.getOrderTime());
        payOrder.setTotalAmount(productEntity.getPrice());
        payOrder.setStatus(orderEntity.getOrderStatusVO().getCode());
        payOrder.setMarketType(MarketTypeVO.NO_MARKET.getCode());
        payOrder.setMarketDeductionAmount(BigDecimal.ZERO);
        payOrder.setPayAmount(productEntity.getPrice());
        payOrder.setMarketType(orderEntity.getMarketType());
        orderDao.insert(payOrder);
    }

    @Override
    public void updateOrderPayInfo(PayOrderEntity payOrderEntity) {
        PayOrder payOrder = PayOrder.builder()
                .userId(payOrderEntity.getUserId())
                .orderId(payOrderEntity.getOrderId())
                .status(payOrderEntity.getOrderStatusVO().getCode())
                .payUrl(payOrderEntity.getPayUrl())
                .marketType(payOrderEntity.getMarketType())
                .marketDeductionAmount(payOrderEntity.getMarketDeductionAmount())
                .payAmount(payOrderEntity.getPayAmount())
                .build();
        orderDao.updateOrderPayInfo(payOrder);
    }

    @Override
    public void changeOrderPaySuccess(String orderId, Date payTime) {
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setStatus(OrderStatusVO.PAY_SUCCESS.getCode());
        payOrder.setPayTime(payTime);
        orderDao.changeOrderPaySuccess(payOrder);
        // 不走拼团营销的直接结算发货
        BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage> paySuccessMessageEventMessage = paySuccessMessageEvent.buildEventMessage(
                PaySuccessMessageEvent.PaySuccessMessage.builder()
                        .tradeNo(orderId)
                        .build());
        PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = paySuccessMessageEventMessage.getData();

        // 旧版发送消息方式
        // eventBus.post(JSON.toJSONString(paySuccessMessage));
        eventPublisher.publish(paySuccessMessageEvent.topic(), JSON.toJSONString(paySuccessMessage));
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderDao.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderDao.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderDao.changeOrderClose(orderId);
    }

    @Override
    public void changeMarketOrderPaySuccess(String orderId) {
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setStatus(OrderStatusVO.PAY_SUCCESS.getCode());
        orderDao.changeOrderPaySuccess(payOrder);
        // 不走拼团营销的直接结算发货
        BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage> paySuccessMessageEventMessage = paySuccessMessageEvent.buildEventMessage(
                PaySuccessMessageEvent.PaySuccessMessage.builder()
                        .tradeNo(orderId)
                        .build());
        PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = paySuccessMessageEventMessage.getData();


        // 旧版发送消息方式
        // eventBus.post(JSON.toJSONString(paySuccessMessage));

        eventPublisher.publish(paySuccessMessageEvent.topic(), JSON.toJSONString(paySuccessMessage));
    }

    @Override
    public OrderEntity queryOrderByOrderId(String orderId) {
        PayOrder payOrder = orderDao.queryOrderByOrderId(orderId);
        if (payOrder == null){return null;}
        return OrderEntity.builder()
                .userId(payOrder.getUserId())
                .productId(payOrder.getProductId())
                .productName(payOrder.getProductName())
                .orderId(payOrder.getOrderId())
                .orderTime(payOrder.getOrderTime())
                .totalAmount(payOrder.getTotalAmount())
                .payUrl(payOrder.getPayUrl())
                .marketType(payOrder.getMarketType())
                .marketDeductionAmount(payOrder.getMarketDeductionAmount())
                .payAmount(payOrder.getPayAmount())
                .build();

    }

    @Override
    public void changeOrderMarketSettlement(List<String> outTradeNoList) {
        // 更新拼团结算状态
        orderDao.changeOrderMarketSettlement(outTradeNoList);
        // 循环成功发送消息 - 一般在公司的场景里，还会有job任务扫描超时没有结算的订单，查询订单状态。查询对方服务端的接口，会被限制一次查询多少，频次多少。
        outTradeNoList.forEach(outTradeNo -> {
            BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage> paySuccessMessageEventMessage = paySuccessMessageEvent.buildEventMessage(
                    PaySuccessMessageEvent.PaySuccessMessage.builder()
                            .tradeNo(outTradeNo)
                            .build());
            PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = paySuccessMessageEventMessage.getData();

            // 旧版发送消息方式
            // eventBus.post(JSON.toJSONString(paySuccessMessage));
            eventPublisher.publish(paySuccessMessageEvent.topic(), JSON.toJSONString(paySuccessMessage));
        });



    }
}

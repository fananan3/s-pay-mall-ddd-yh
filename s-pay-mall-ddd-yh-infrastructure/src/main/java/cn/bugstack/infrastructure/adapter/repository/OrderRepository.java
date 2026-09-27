package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ProductEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.infrastructure.dao.IOrderDao;
import cn.bugstack.infrastructure.dao.po.PayOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        orderDao.insert(payOrder);
    }

    @Override
    public void updateOrderPayInfo(PayOrderEntity payOrderEntity) {
        PayOrder payOrder = PayOrder.builder()
                .userId(payOrderEntity.getUserId())
                .orderId(payOrderEntity.getOrderId())
                .status(payOrderEntity.getOrderStatusVO().getCode())
                .payUrl(payOrderEntity.getPayUrl())
                .build();
        orderDao.updateOrderPayInfo(payOrder);
    }
}

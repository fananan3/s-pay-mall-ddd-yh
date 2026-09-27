package cn.bugstack.domain.order.adapter.repository;

import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;

import java.util.List;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 17:48
 */
public interface IOrderRepository {
    OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    void doSaveOrder(CreateOrderAggregate createOrderAggregate);

    void updateOrderPayInfo(PayOrderEntity payOrderEntity);

    void changeOrderPaySuccess(String orderId);
    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);
}

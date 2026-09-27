package cn.bugstack.domain.order.adapter.repository;

import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 17:48
 */
public interface IOrderRepository {
    OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    void doSaveOrder(CreateOrderAggregate createOrderAggregate);
}

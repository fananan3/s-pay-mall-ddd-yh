package cn.bugstack.domain.order.service;


import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;

import java.util.List;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 21:03
 */
public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

    void changeOrderPaySuccess(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);
}

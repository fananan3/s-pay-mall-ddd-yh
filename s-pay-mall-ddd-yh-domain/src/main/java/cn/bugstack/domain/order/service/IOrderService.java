package cn.bugstack.domain.order.service;


import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import com.alipay.api.AlipayApiException;

import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 21:03
 */
public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

    void changeOrderPaySuccess(String orderId, Date payTime);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId );

    void changeOrderMarketSettlement(List<String> outTradeNoList);

    List<OrderEntity> queryUserOrderList(String userId, Long lastId, Integer pageSize);

    boolean refundOrder(String userId, String orderId);

    /**
     * 营销退单
     */
    boolean refundMarketOrder(String userId, String orderId);


    /**
     * 接收拼团退单消息
     */
    boolean refundPayOrder(String userId, String outTradeNo) throws AlipayApiException;

    OrderEntity queryOrder(String userId, String orderId);

    void closeRefundOrder(String userId, String outTradeNo);
}

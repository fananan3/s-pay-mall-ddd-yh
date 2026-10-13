package cn.bugstack.infrastructure.dao;

import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.infrastructure.dao.po.PayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 21:03
 */
@Mapper
public interface IOrderDao {
    void insert(PayOrder payOrder);

    PayOrder queryUnPayOrder(PayOrder payOrder);

    void updateOrderPayInfo(PayOrder PayOrder);

    void changeOrderPaySuccess(PayOrder order);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

    PayOrder queryOrderByOrderId(String orderId);

    void changeOrderMarketSettlement(List<String> outTradeNoList);

    void changeOrderDealDone(String tradeNo);

    /**
     * 我的订单列表（游标分页）
     * 多参数必须加 @Param：否则 XML 里 #{userId}/#{lastId}/#{pageSize} 解析不到，
     * 会报 Parameter 'lastId' not found. Available parameters are [arg0, arg1, arg2...]
     */
    List<PayOrder> queryUserOrderList(@Param("userId") String userId,
                                      @Param("lastId") Long lastId,
                                      @Param("pageSize") Integer pageSize);

    PayOrder queryOrderByUserIdAndOrderId(@Param("userId") String userId,
                                          @Param("orderId") String orderIdString );

    boolean refundOrder(@Param("userId") String userId, @Param("orderId") String orderId);

    boolean refundMarketOrder(@Param("userId") String userId, @Param("orderId") String orderId);

    boolean closeRefundOrder(String userId, String outTradeNo);

    void changeMarketOrderWaitSettlement(PayOrder payOrder);

    List<PayOrder> queryWaitRefundOrderList();

    boolean changeOrderWaitRefund(@Param("userId") String userId, @Param("orderId") String orderId);
}

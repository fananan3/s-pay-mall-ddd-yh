package cn.bugstack.infrastructure.dao;

import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.infrastructure.dao.po.PayOrder;
import org.apache.ibatis.annotations.Mapper;

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
}

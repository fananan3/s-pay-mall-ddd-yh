package cn.bugstack.domain.order.service.impl;

import cn.bugstack.domain.order.adapter.port.IProductPort;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
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


    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    public OrderService(IOrderRepository repository, IProductPort port){
        super(repository,port);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);
        bizContent.put("total_amount", totalAmount.toString());
        bizContent.put("subject", productName);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        String form = alipayClient.pageExecute(request).getBody();

        PayOrderEntity payOrderEntity = new PayOrderEntity();
        payOrderEntity.setOrderId(orderId);
        payOrderEntity.setPayUrl(form);
        payOrderEntity.setOrderStatusVO(OrderStatusVO.CREATE);

        orderRepository.updateOrderPayInfo(payOrderEntity);
        return payOrderEntity;
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        orderRepository.doSaveOrder(createOrderAggregate);
    }

    @Override
    public void changeOrderPaySuccess(String orderId) {
        orderRepository.changeOrderPaySuccess(orderId);
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
}

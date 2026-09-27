package cn.bugstack.trigger.job;


import cn.bugstack.domain.order.service.IOrderService;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 检测未接收到或未正确处理的支付回调通知
 * @create 2024-09-30 09:59
 */
@Slf4j
@Component()
public class NoPayNotifyOrderJob {

    @Resource
    private IOrderService orderService;
    @Resource
    private AlipayClient alipayClient;

    @Scheduled(cron = "0/3 * * * * ?")
    public void exec() {
        try {
            log.info("任务；检测未接收到或未正确处理的支付回调通知");
            List<String> orderIds = orderService.queryNoPayNotifyOrder();
            if (null == orderIds || orderIds.isEmpty()) {return;}

            for (String orderId : orderIds) {
                AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
                AlipayTradeQueryModel bizModel = new AlipayTradeQueryModel();
                bizModel.setOutTradeNo(orderId);
                request.setBizModel(bizModel);

                AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (response == null) {
                    log.error("订单 {} 查询支付宝返回 null，可能网络或配置异常", orderId);
                    continue;
                }
                String code = response.getCode();
                if (!"10000".equals(code)){
                    log.warn("订单 {} 查询失败：code={}, subCode={}, subMsg={}",orderId, code, response.getSubCode(), response.getSubMsg());
                    // 交易不存在 → 用户根本没付款，可以考虑关单，但不要改支付成功
                    continue;
                }
                String tradeStatus = response.getTradeStatus();

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    orderService.changeOrderPaySuccess(orderId);
                    log.info("订单 {} 补偿成功，交易状态：{}", orderId, tradeStatus);
                } else {
                    log.info("订单 {} 交易状态：{}，暂不处理", orderId, tradeStatus);
                }
            }
        } catch (Exception e) {
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }

}

package cn.bugstack.trigger.job;

import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 结算补偿任务：扫描「待结算(WAIT_SETTLEMENT)」的已支付订单，重试拼团结算直至成功
 * @Author fanyiheng
 * @Date 2026/10/13 18:20
 * @note 背景：营销单支付成功后调拼团结算可能因网络等瞬时原因失败，此时订单标记 WAIT_SETTLEMENT；
 *       若只依赖 order_pay_success 消息重投，该消息只发一次，消费失败后无人再触发结算，订单会卡在
 *       「已支付未结算」。本任务每 1 分钟扫描 WAIT_SETTLEMENT 订单，用「原始支付时间」重跑
 *       changeOrderPaySuccess（内部会再次调拼团结算）：
 *       结算成功 → PAY_SUCCESS；业务被拒(E0105/E0106) → 自动退款；仍失败 → 留 WAIT_SETTLEMENT 下轮再试。
 */
@Slf4j
@Service
public class SettlementRetryJob {

    @Resource
    private IOrderService orderService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void exec() {
        try {
            List<OrderEntity> waitSettlementOrders = orderService.queryWaitSettlementOrderList();
            if (null == waitSettlementOrders || waitSettlementOrders.isEmpty()) {
                return;
            }
            for (OrderEntity order : waitSettlementOrders) {
                try {
                    orderService.changeOrderPaySuccess(order.getOrderId(), order.getPayTime());
                    log.info("结算补偿，订单 {} 重试结算完成", order.getOrderId());
                } catch (Exception e) {
                    log.error("结算补偿失败 orderId:{}", order.getOrderId(), e);
                }
            }
        } catch (Exception e) {
            log.error("结算补偿任务执行异常", e);
        }
    }

}

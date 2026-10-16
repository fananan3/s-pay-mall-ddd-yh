package cn.bugstack.trigger.job;

import cn.bugstack.domain.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description 商城订单 → 分析库(analytics_db) 定时增量同步
 * @Author fanyiheng
 * @Date 2026/10/13 20:10
 * @note 每天凌晨低峰把 pay_order 增量同步到 analytics_db（明细 + 用户聚合），
 *       供其它微服务做全量用户交易的离线分析/人群标签。断点续跑（last_order_id）。
 */
@Slf4j
@Service
public class OrderAnalyticsSyncJob {

    @Resource
    private IOrderService orderService;

    @Scheduled(cron = "0 30 3 * * ?")
    public void exec() {
        try {
            orderService.syncOrdersToAnalytics();
            log.info("商城订单同步分析库完成");
        } catch (Exception e) {
            log.error("商城订单同步分析库失败", e);
        }
    }

}

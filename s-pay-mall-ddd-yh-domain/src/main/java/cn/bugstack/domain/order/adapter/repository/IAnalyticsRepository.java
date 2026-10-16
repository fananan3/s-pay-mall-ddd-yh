package cn.bugstack.domain.order.adapter.repository;

import cn.bugstack.domain.order.model.entity.OrderEntity;

/**
 * @Description 分析库仓储：商城订单 → 分析库（analytics_db），供全量用户交易离线分析/人群标签
 * @Author fanyiheng
 * @Date 2026/10/13 20:05
 */
public interface IAnalyticsRepository {

    /** 读取增量同步断点（已同步到的最大 order_id） */
    Long getSyncCheckpoint();

    /** 更新增量同步断点 */
    void updateSyncCheckpoint(Long lastOrderId);

    /** upsert 一笔订单明细到分析库 fact_user_order（按 order_id 幂等） */
    void upsertFactOrder(OrderEntity order);

    /** 重算用户交易聚合 user_trade_summary（GROUP BY user_id） */
    void recomputeUserSummary();

}

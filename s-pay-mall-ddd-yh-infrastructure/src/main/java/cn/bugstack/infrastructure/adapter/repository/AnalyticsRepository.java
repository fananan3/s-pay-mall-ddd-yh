package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.order.adapter.repository.IAnalyticsRepository;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 分析库仓储实现（JdbcTemplate，直连 analytics_db）
 * @Author fanyiheng
 * @Date 2026/10/13 20:05
 */
@Slf4j
@Repository
public class AnalyticsRepository implements IAnalyticsRepository {

    @Resource
    @Qualifier("analyticsJdbcTemplate")
    private JdbcTemplate analyticsJdbcTemplate;

    @Override
    public Long getSyncCheckpoint() {
        List<Long> list = analyticsJdbcTemplate.query(
                "SELECT last_order_id FROM sync_checkpoint WHERE id = 1",
                (rs, i) -> rs.getLong(1));
        return list.isEmpty() ? 0L : list.get(0);
    }

    @Override
    public void updateSyncCheckpoint(Long lastOrderId) {
        analyticsJdbcTemplate.update(
                "UPDATE sync_checkpoint SET last_order_id = ?, last_sync_time = NOW() WHERE id = 1",
                lastOrderId);
    }

    @Override
    public void upsertFactOrder(OrderEntity order) {
        Integer orderType = MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(order.getMarketType()) ? 2 : 1;
        analyticsJdbcTemplate.update(
                "INSERT INTO fact_user_order(user_id, order_id, order_type, amount, order_time, pay_time, status, source_db) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, 'mall') " +
                        "ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status), " +
                        "pay_time = VALUES(pay_time), order_time = VALUES(order_time)",
                order.getUserId(), order.getOrderId(), orderType, order.getPayAmount(),
                order.getOrderTime(), order.getPayTime(),
                null == order.getOrderStatusVO() ? null : order.getOrderStatusVO().getCode());
    }

    @Override
    public void recomputeUserSummary() {
        analyticsJdbcTemplate.update(
                "INSERT INTO user_trade_summary(user_id, total_amount, total_count, market_count, single_count, last_pay_time) " +
                        "SELECT user_id, SUM(amount), COUNT(*), SUM(order_type = 2), SUM(order_type = 1), MAX(pay_time) " +
                        "FROM fact_user_order GROUP BY user_id " +
                        "ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount), total_count = VALUES(total_count), " +
                        "market_count = VALUES(market_count), single_count = VALUES(single_count), last_pay_time = VALUES(last_pay_time)");
    }

}

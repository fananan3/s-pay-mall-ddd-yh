package cn.bugstack.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @Description 多数据源：主库（业务读写，spring.datasource.*）+ 分析库（离线分析只读，spring.datasource.analytics.*）
 * @Author fanyiheng
 * @Date 2026/10/13 20:00
 * @note 主库保持 @Primary 让 MyBatis 自动装配不受影响；分析库用独立 DataSource + JdbcTemplate，
 *       供「商城订单 → 分析库」离线同步使用，避免与业务库混用。
 */
@Configuration
public class AnalyticsDataSourceConfig {

    /** 主数据源：业务库，MyBatis 默认使用 */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    /** 分析库数据源：离线分析只读库 */
    @Bean(name = "analyticsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "analyticsJdbcTemplate")
    public JdbcTemplate analyticsJdbcTemplate(@Qualifier("analyticsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}

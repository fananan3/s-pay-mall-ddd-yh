package cn.bugstack.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @Description 多数据源：主库（业务读写，spring.datasource.*）+ 分析库（离线分析只读，spring.datasource.analytics.*）
 * @Author fanyiheng
 * @Date 2026/10/13 20:00
 * @note 主库用 DataSourceProperties.initializeDataSourceBuilder() 保证 Hikari jdbcUrl 正确绑定，
 *       分析库用显式 setJdbcUrl + JdbcTemplate 供「商城订单 → 分析库」离线同步使用。
 */
@Configuration
public class AnalyticsDataSourceConfig {

    /** 主数据源：业务库，MyBatis 默认使用 */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties("spring.datasource")
    public DataSource primaryDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    /** 分析库数据源：离线分析只读库 */
    @Bean(name = "analyticsDataSource")
    public DataSource analyticsDataSource(
            @Value("${spring.datasource.analytics.url}") String url,
            @Value("${spring.datasource.analytics.username}") String username,
            @Value("${spring.datasource.analytics.password}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "analyticsJdbcTemplate")
    public JdbcTemplate analyticsJdbcTemplate(@Qualifier("analyticsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}

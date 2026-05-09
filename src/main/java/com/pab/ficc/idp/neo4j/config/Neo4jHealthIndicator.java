package com.pab.ficc.idp.neo4j.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceStatValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Neo4j 数据源健康检查，暴露到 /actuator/health。
 * health key 为 "neo4j"（Actuator 取类名去掉 HealthIndicator 后缀）
 */
@Slf4j
@Component
public class Neo4jHealthIndicator implements HealthIndicator {

    private static final String PING_CYPHER = "RETURN 1";
    private static final int PING_TIMEOUT_SECONDS = 3;

    private final DataSource dataSource;

    // @Qualifier 必须加在构造器参数上，@RequiredArgsConstructor 不支持字段级 @Qualifier
    public Neo4jHealthIndicator(@Qualifier("neo4jDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(PING_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(PING_CYPHER)) {
                rs.next();
            }

            Health.Builder builder = Health.up();
            appendPoolStats(builder);
            return builder.build();

        } catch (Exception e) {
            log.warn("[Neo4j] Health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("url", getUrl())
                    .build();
        }
    }

    private void appendPoolStats(Health.Builder builder) {
        if (dataSource instanceof DruidDataSource ds) {
            DruidDataSourceStatValue stat = ds.getStatValueAndReset();
            builder.withDetail("url", ds.getUrl())
                   .withDetail("activeCount", ds.getActiveCount())
                   .withDetail("poolingCount", ds.getPoolingCount())
                   .withDetail("maxActive", ds.getMaxActive())
                   .withDetail("waitThreadCount", ds.getWaitThreadCount())
                   .withDetail("connectErrorCount", stat.getPhysicalConnectErrorCount());
        }
    }

    private String getUrl() {
        return (dataSource instanceof DruidDataSource ds) ? ds.getUrl() : "unknown";
    }
}

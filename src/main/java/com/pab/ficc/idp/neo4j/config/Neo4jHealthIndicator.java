package com.pab.ficc.idp.neo4j.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceStatValue;
import lombok.RequiredArgsConstructor;
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
 * <p>
 * 检查内容：
 * 1. 执行 {@code RETURN 1} 验证 Neo4j 服务可达
 * 2. 上报 Druid 连接池关键指标（active / idle / wait）
 */
@Slf4j
@Component("neo4jDataSource")   // Actuator 会以 Bean 名拼接生成 health key: neo4jDataSource
@RequiredArgsConstructor
public class Neo4jHealthIndicator implements HealthIndicator {

    private static final String PING_CYPHER = "RETURN 1";
    private static final int PING_TIMEOUT_SECONDS = 3;

    @Qualifier("neo4jDataSource")
    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(PING_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(PING_CYPHER)) {
                rs.next(); // 正常情况下一定有一行
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

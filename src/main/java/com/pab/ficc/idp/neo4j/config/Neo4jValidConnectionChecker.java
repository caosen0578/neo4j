package com.pab.ficc.idp.neo4j.config;

import com.alibaba.druid.pool.ValidConnectionChecker;
import com.alibaba.druid.pool.ValidConnectionCheckerAdapter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * Neo4j JDBC 专用连接探活实现。
 * <p>
 * Druid 默认用 {@code validationQuery} 走 JDBC executeQuery，
 * Neo4j JDBC 支持 Cypher，用 {@code RETURN 1} 替代标准 {@code SELECT 1}。
 */
@Slf4j
public class Neo4jValidConnectionChecker extends ValidConnectionCheckerAdapter
        implements ValidConnectionChecker {

    private static final String PING_CYPHER = "RETURN 1";

    @Override
    public boolean isValidConnection(Connection conn, String validationQuery,
                                     int validationQueryTimeout) {
        try {
            if (conn.isClosed()) {
                return false;
            }
            try (Statement stmt = conn.createStatement()) {
                if (validationQueryTimeout > 0) {
                    stmt.setQueryTimeout(validationQueryTimeout);
                }
                try (ResultSet rs = stmt.executeQuery(PING_CYPHER)) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            log.warn("[Neo4j] Connection validation failed, will evict: {}", e.getMessage());
            return false;
        }
    }
}

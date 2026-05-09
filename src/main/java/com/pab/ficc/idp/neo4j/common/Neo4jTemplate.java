package com.pab.ficc.idp.neo4j.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Neo4j JDBC 操作模板，封装连接获取与释放，屏蔽 JDBC 样板代码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Neo4jTemplate {

    @Qualifier("neo4jDataSource")
    private final DataSource dataSource;

    /**
     * 执行 Cypher 查询，返回结果集（每行为 column→value 的 Map）。
     *
     * @param cypher Cypher 查询语句
     * @param params 位置参数（对应 Cypher 中的 $1, $2 ...）
     */
    public List<Map<String, Object>> query(String cypher, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(cypher)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            throw new Neo4jQueryException("Cypher query failed: " + cypher, e);
        }
        return results;
    }

    /**
     * 执行写操作（CREATE / MERGE / SET / DELETE），返回受影响节点/关系数。
     */
    public int update(String cypher, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(cypher)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new Neo4jQueryException("Cypher update failed: " + cypher, e);
        }
    }
}

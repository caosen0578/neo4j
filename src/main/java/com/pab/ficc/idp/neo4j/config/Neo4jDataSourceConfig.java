package com.pab.ficc.idp.neo4j.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Neo4jDataSourceConfig {

    private static final String NEO4J_DRIVER = "org.neo4j.jdbc.bolt.BoltDriver";

    private final Neo4jDataSourceProperties props;

    @Bean(name = "neo4jDataSource", initMethod = "init", destroyMethod = "close")
    public DataSource neo4jDataSource() throws SQLException {
        Neo4jDataSourceProperties.Druid druid = props.getDruid();

        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        ds.setDriverClassName(NEO4J_DRIVER);

        // ===== 连接池大小 =====
        ds.setInitialSize(druid.getInitialSize());
        ds.setMinIdle(druid.getMinIdle());
        ds.setMaxActive(druid.getMaxActive());
        ds.setMaxWait(druid.getMaxWait());
        ds.setTimeBetweenEvictionRunsMillis(druid.getTimeBetweenEvictionRunsMillis());
        ds.setMinEvictableIdleTimeMillis(druid.getMinEvictableIdleTimeMillis());

        // ===== 健康探活（用自定义 ValidConnectionChecker 执行 RETURN 1）=====
        ds.setValidConnectionChecker(new Neo4jValidConnectionChecker());
        ds.setValidationQuery("RETURN 1");      // 日志展示用，实际由 Checker 接管
        ds.setTestWhileIdle(druid.isTestWhileIdle());
        ds.setTestOnBorrow(druid.isTestOnBorrow());
        ds.setTestOnReturn(false);

        // ===== keepAlive 心跳（防止 Neo4j 服务端踢掉空闲连接）=====
        ds.setKeepAlive(druid.isKeepAlive());
        ds.setKeepAliveBetweenTimeMillis(druid.getKeepAliveBetweenTimeMillis());

        // ===== 断线重连 =====
        // breakAfterAcquireFailure=false：获取连接失败后连接池继续工作，不废弃整个池
        ds.setBreakAfterAcquireFailure(druid.isBreakAfterAcquireFailure());
        // 连接错误后的重试次数与等待间隔
        ds.setConnectionErrorRetryAttempts(druid.getConnectionErrorRetryAttempts());
        ds.setTimeBetweenConnectErrorMillis(druid.getTimeBetweenConnectErrorMillis());

        // ===== 监控 Filter =====
        // 注意：WallFilter 需要识别 dbType（MySQL/Oracle 等），Neo4j 不在支持列表内
        // 会导致 dbType=null NPE，因此只启用 StatFilter 用于慢查询统计
        if (druid.isStatEnable()) {
            StatFilter statFilter = new StatFilter();
            statFilter.setSlowSqlMillis(druid.getSlowSqlMillis());
            statFilter.setLogSlowSql(true);
            statFilter.setMergeSql(true);

            ds.setProxyFilters(List.of(statFilter));
        }

        log.info("[Neo4j] DruidDataSource initialized — url={}, maxActive={}, keepAlive={}, retryAttempts={}",
                props.getUrl(), druid.getMaxActive(), druid.isKeepAlive(),
                druid.getConnectionErrorRetryAttempts());
        return ds;
    }
}

package com.pab.ficc.idp.neo4j.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.pab.ficc.idp.neo4j.common.yaml.YamlConfigFlattener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class Neo4jDataSourceConfig {

    private static final String NEO4J_DRIVER = "org.neo4j.jdbc.bolt.BoltDriver";

    /**
     * 与业务代码 @Value("${datasource.namespace:neo4j_datasource_config}") 保持一致，
     * namespace 名称在 bootstrap.yml 的 apollo.bootstrap.namespaces 中声明。
     */
    @Value("${datasource.namespace:neo4j_datasource_config}")
    private String neo4jNamespace;

    private final Environment environment;

    public Neo4jDataSourceConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean(name = "neo4jDataSource", initMethod = "init", destroyMethod = "close")
    public DataSource neo4jDataSource() throws SQLException {
        Neo4jDataSourceProperties props = loadProperties();
        Neo4jDataSourceProperties.Druid druid = props.getDruid();

        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        ds.setDriverClassName(NEO4J_DRIVER);

        ds.setInitialSize(druid.getInitialSize());
        ds.setMinIdle(druid.getMinIdle());
        ds.setMaxActive(druid.getMaxActive());
        ds.setMaxWait(druid.getMaxWait());
        ds.setTimeBetweenEvictionRunsMillis(druid.getTimeBetweenEvictionRunsMillis());
        ds.setMinEvictableIdleTimeMillis(druid.getMinEvictableIdleTimeMillis());

        ds.setValidConnectionChecker(new Neo4jValidConnectionChecker());
        ds.setValidationQuery("RETURN 1");
        ds.setTestWhileIdle(druid.isTestWhileIdle());
        ds.setTestOnBorrow(druid.isTestOnBorrow());
        ds.setTestOnReturn(false);

        ds.setKeepAlive(druid.isKeepAlive());
        ds.setKeepAliveBetweenTimeMillis(druid.getKeepAliveBetweenTimeMillis());

        ds.setBreakAfterAcquireFailure(druid.isBreakAfterAcquireFailure());
        ds.setConnectionErrorRetryAttempts(druid.getConnectionErrorRetryAttempts());
        ds.setTimeBetweenConnectErrorMillis(druid.getTimeBetweenConnectErrorMillis());

        if (druid.isStatEnable()) {
            StatFilter statFilter = new StatFilter();
            statFilter.setSlowSqlMillis(druid.getSlowSqlMillis());
            statFilter.setLogSlowSql(true);
            statFilter.setMergeSql(true);
            ds.setProxyFilters(List.of(statFilter));
        }

        log.info("[Neo4j] DruidDataSource initialized — url={}, maxActive={}",
                props.getUrl(), druid.getMaxActive());
        return ds;
    }

    /**
     * 配置加载策略：
     * 1. 优先从 Apollo namespace 读取（ConfigService.getConfig，Bean 创建阶段可靠）
     * 2. Apollo 不可用时回退到 Spring Environment（application.yml 本地兜底）
     */
    private Neo4jDataSourceProperties loadProperties() {
        Map<String, Object> apolloProps = loadFromApollo();

        if (!apolloProps.isEmpty()) {
            log.info("[Neo4j] Config loaded from Apollo namespace='{}'", neo4jNamespace);
            return new Binder(new MapConfigurationPropertySource(apolloProps))
                    .bind("neo4j.datasource", Bindable.of(Neo4jDataSourceProperties.class))
                    .orElse(new Neo4jDataSourceProperties());
        }

        // 回退：直接从当前 Environment 绑定（application.yml）
        log.info("[Neo4j] Apollo unavailable, config loaded from local application.yml");
        return Binder.get(environment)
                .bind("neo4j.datasource", Bindable.of(Neo4jDataSourceProperties.class))
                .orElse(new Neo4jDataSourceProperties());
    }

    /**
     * 从 Apollo namespace 读取 YAML 内容并打平为属性 Map。
     * Apollo portal namespace 类型须为 yaml。
     */
    private Map<String, Object> loadFromApollo() {
        try {
            // 依次尝试 yaml / yml，两种格式内容相同，仅 Apollo portal namespace 类型名称不同
            ConfigFile configFile = ConfigService.getConfigFile(neo4jNamespace, ConfigFileFormat.YAML);
            if (configFile == null || !configFile.hasContent()) {
                configFile = ConfigService.getConfigFile(neo4jNamespace, ConfigFileFormat.YML);
            }
            if (configFile == null || !configFile.hasContent()) {
                log.warn("[Neo4j] Apollo namespace='{}' has no content", neo4jNamespace);
                return Map.of();
            }
            Map<String, Object> result = YamlConfigFlattener.flatten(configFile.getContent());
            log.info("[Neo4j] Apollo namespace='{}' loaded, keys={}", neo4jNamespace, result.keySet());
            return result;
        } catch (Exception e) {
            log.warn("[Neo4j] Failed to load from Apollo namespace='{}': {}", neo4jNamespace, e.getMessage());
            return Map.of();
        }
    }
}

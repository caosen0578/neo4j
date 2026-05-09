package com.pab.ficc.idp.neo4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "neo4j.datasource")
public class Neo4jDataSourceProperties {

    private String url;
    private String username;
    private String password;

    private Druid druid = new Druid();

    @Data
    public static class Druid {
        private int initialSize = 2;
        private int minIdle = 2;
        private int maxActive = 10;
        /** 获取连接最大等待时间(ms) */
        private long maxWait = 60_000;
        /** 检测空闲连接间隔(ms) */
        private long timeBetweenEvictionRunsMillis = 60_000;
        /** 连接最小存活时间(ms) */
        private long minEvictableIdleTimeMillis = 300_000;

        // ===== 健康探活 =====
        /** 空闲连接探活开关，依赖 validConnectionChecker */
        private boolean testWhileIdle = true;
        /** 借出连接前探活（影响性能，生产建议 false） */
        private boolean testOnBorrow = false;
        /** keepAlive 心跳，防止空闲连接被服务端踢掉 */
        private boolean keepAlive = true;
        /** keepAlive 心跳间隔(ms)，必须大于 timeBetweenEvictionRunsMillis */
        private long keepAliveBetweenTimeMillis = 120_000;

        // ===== 断线重连 =====
        /** 获取连接失败后不立即废弃连接池，允许重试 */
        private boolean breakAfterAcquireFailure = false;
        /** 连接失败后重试次数 */
        private int connectionErrorRetryAttempts = 3;
        /** 两次重试之间等待时间(ms) */
        private long timeBetweenConnectErrorMillis = 500;

        // ===== 监控 =====
        /** 开启 Druid 监控统计 */
        private boolean statEnable = true;
        /** 慢查询记录阈值(ms) */
        private long slowSqlMillis = 3_000;
    }
}

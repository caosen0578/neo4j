package com.pab.ficc.idp.neo4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// 排除 Spring Boot 默认数据源自动装配，由 Neo4jDataSourceConfig 手动创建
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Neo4jServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Neo4jServiceApplication.class, args);
    }
}

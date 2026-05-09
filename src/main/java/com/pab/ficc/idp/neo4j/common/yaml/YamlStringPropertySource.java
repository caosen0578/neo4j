package com.pab.ficc.idp.neo4j.common.yaml;

import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * 将 YAML 字符串解析后封装为 Spring {@link MapPropertySource}，
 * 可注册到 {@code ConfigurableEnvironment} 供 {@code @ConfigurationProperties} 绑定。
 */
public class YamlStringPropertySource extends MapPropertySource {

    /**
     * @param name        PropertySource 名称，需在同一 Environment 中唯一
     * @param yamlContent YAML 格式字符串
     */
    public YamlStringPropertySource(String name, String yamlContent) {
        super(name, YamlConfigFlattener.flatten(yamlContent));
    }

    /**
     * 直接使用已打平的 Map 构造（供测试或二次加工使用）。
     */
    public YamlStringPropertySource(String name, Map<String, Object> flatProperties) {
        super(name, flatProperties);
    }
}

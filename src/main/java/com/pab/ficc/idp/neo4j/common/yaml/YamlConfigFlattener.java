package com.pab.ficc.idp.neo4j.common.yaml;

import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 YAML 字符串安全解析并打平为 Spring 可识别的 dot-notation 属性 Map。
 *
 * <pre>
 * 输入 YAML：
 *   neo4j:
 *     datasource:
 *       url: jdbc:neo4j:bolt://host:7687
 *       druid:
 *         max-active: 10
 *
 * 输出 Map：
 *   neo4j.datasource.url              = jdbc:neo4j:bolt://host:7687
 *   neo4j.datasource.druid.max-active = 10
 * </pre>
 *
 * <p><b>安全说明：</b>使用 {@link SafeConstructor} 限制只允许解析基本类型
 * （String / Number / Boolean / List / Map），禁止 {@code !!com.example.Foo}
 * 等类型标签，防止不安全反序列化（CWE-502）。同时限制输入大小防 DoS。
 */
public final class YamlConfigFlattener {

    /** YAML 内容最大字节数（1 MB），防止超大输入导致 OOM */
    private static final int MAX_YAML_CODE_POINT_LIMIT = 1024 * 1024;

    private YamlConfigFlattener() {}

    /**
     * 解析 YAML 字符串，返回打平后的不可修改属性 Map。
     *
     * @param yamlContent YAML 格式字符串，为空时返回空 Map
     * @return dot-notation 属性 Map
     */
    public static Map<String, Object> flatten(String yamlContent) {
        if (!StringUtils.hasText(yamlContent)) {
            return Collections.emptyMap();
        }

        Object loaded = buildSafeYaml().load(yamlContent);
        if (!(loaded instanceof Map<?, ?> rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        flattenMap("", rawMap, result);
        return Collections.unmodifiableMap(result);
    }

    private static Yaml buildSafeYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setCodePointLimit(MAX_YAML_CODE_POINT_LIMIT);
        return new Yaml(new SafeConstructor(options));
    }

    private static void flattenMap(String prefix, Map<?, ?> source, Map<String, Object> result) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = prefix.isEmpty()
                    ? String.valueOf(entry.getKey())
                    : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flattenMap(key, nested, result);
            } else {
                result.put(key, value == null ? null : String.valueOf(value));
            }
        }
    }
}

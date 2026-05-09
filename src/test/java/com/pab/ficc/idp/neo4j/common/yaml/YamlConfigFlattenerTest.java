package com.pab.ficc.idp.neo4j.common.yaml;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigFlattenerTest {

    private static final String SAMPLE_YAML = """
            neo4j:
              datasource:
                url: jdbc:neo4j:bolt://29.23.14.250:7687
                username: deployop
                password: 0p3Gy7Tf5R
                druid:
                  initial-size: 2
                  max-active: 10
                  keep-alive: true
                  keep-alive-between-time-millis: 120000
            """;

    @Test
    void flatten_shouldProduceDotNotationKeys() {
        Map<String, Object> props = YamlConfigFlattener.flatten(SAMPLE_YAML);
        assertEquals("jdbc:neo4j:bolt://29.23.14.250:7687", props.get("neo4j.datasource.url"));
        assertEquals("deployop",    props.get("neo4j.datasource.username"));
        assertEquals("10",          props.get("neo4j.datasource.druid.max-active"));
        assertEquals("true",        props.get("neo4j.datasource.druid.keep-alive"));
        assertEquals("120000",      props.get("neo4j.datasource.druid.keep-alive-between-time-millis"));
    }

    @Test
    void flatten_nullOrBlank_shouldReturnEmptyMap() {
        assertTrue(YamlConfigFlattener.flatten(null).isEmpty());
        assertTrue(YamlConfigFlattener.flatten("").isEmpty());
        assertTrue(YamlConfigFlattener.flatten("   ").isEmpty());
    }

    @Test
    void flatten_returnedMap_shouldBeImmutable() {
        Map<String, Object> props = YamlConfigFlattener.flatten(SAMPLE_YAML);
        assertThrows(UnsupportedOperationException.class, () -> props.put("hack", "value"));
    }

    @Test
    void flatten_withTypeTag_shouldThrowException() {
        String maliciousYaml = "key: !!com.example.Exploit\n  field: value\n";
        assertThrows(ConstructorException.class, () -> YamlConfigFlattener.flatten(maliciousYaml));
    }

    @Test
    void flatten_duplicateKey_shouldThrowException() {
        String duplicateYaml = "neo4j:\n  url: first\nneo4j:\n  url: second\n";
        assertThrows(Exception.class, () -> YamlConfigFlattener.flatten(duplicateYaml));
    }
}

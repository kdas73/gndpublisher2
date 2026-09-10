package com.gnd.publisher.integration.llm;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Schema definitions for the model-facing response contracts, shared by every provider.
 *
 * <p>Schemas are built with {@link LinkedHashMap} rather than {@code Map.of} so that property order
 * is stable across JVM runs. Ollama compiles the schema into a grammar, where iteration order is
 * observable in the generated output.
 */
public final class LlmResponseSchemas {

    private LlmResponseSchemas() {
    }

    public static Map<String, Object> classificationResponseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("primaryCategoryCode", stringSchema());
        properties.put("categoryCodes", arraySchema(stringSchema()));
        properties.put("semanticKey", stringSchema());
        properties.put("semanticKeyAction", enumSchema("matched_existing", "created_new"));
        properties.put("matchedSemanticEventId", nullableStringSchema());
        properties.put("confidence", numberSchema());
        properties.put("shouldPublish", booleanSchema());
        // Arrays.asList, not List.of: this enum deliberately contains a null value.
        properties.put("rejectionReason", Collections.singletonMap("enum", Arrays.asList(
                "EDITORIAL_RULE_EXCLUDED",
                "NOT_PUBLISHABLE_CATEGORY",
                "SOURCE_RUN_QUOTA_EXCEEDED",
                "DUPLICATE_SEMANTIC_EVENT",
                "LOW_CONFIDENCE",
                "CLASSIFICATION_FAILED",
                null)));

        return objectSchema(properties, List.copyOf(properties.keySet()));
    }

    public static Map<String, Object> publicationContentResponseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", stringSchema());
        properties.put("summary", stringSchema());
        properties.put("confidence", numberSchema());

        return objectSchema(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return Collections.unmodifiableMap(schema);
    }

    private static Map<String, Object> arraySchema(Map<String, Object> items) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", items);
        return Collections.unmodifiableMap(schema);
    }

    private static Map<String, Object> enumSchema(String... values) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("enum", List.of(values));
        return Collections.unmodifiableMap(schema);
    }

    private static Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private static Map<String, Object> nullableStringSchema() {
        return Map.of("type", List.of("string", "null"));
    }

    private static Map<String, Object> booleanSchema() {
        return Map.of("type", "boolean");
    }

    private static Map<String, Object> numberSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "number");
        schema.put("minimum", 0.0);
        schema.put("maximum", 1.0);
        return Collections.unmodifiableMap(schema);
    }
}

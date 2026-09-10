package com.gnd.publisher.dto.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gnd.publisher.exception.LlmIntegrationException;

public enum SemanticKeyActionDto {
    MATCHED_EXISTING("matched_existing"),
    CREATED_NEW("created_new");

    private final String jsonValue;

    SemanticKeyActionDto(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }

    /**
     * Parsing is case-insensitive and tolerates surrounding whitespace, because smaller local
     * models vary the casing of enum values even under a constrained schema.
     */
    @JsonCreator
    public static SemanticKeyActionDto fromJson(String value) {
        if (value != null) {
            String normalized = value.trim();
            for (SemanticKeyActionDto action : values()) {
                if (action.jsonValue.equalsIgnoreCase(normalized) || action.name().equalsIgnoreCase(normalized)) {
                    return action;
                }
            }
        }
        throw new LlmIntegrationException("Unknown semanticKeyAction value: " + value);
    }
}

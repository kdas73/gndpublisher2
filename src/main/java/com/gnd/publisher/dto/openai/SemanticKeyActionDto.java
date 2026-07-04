package com.gnd.publisher.dto.openai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gnd.publisher.exception.OpenAiIntegrationException;

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

    @JsonCreator
    public static SemanticKeyActionDto fromJson(String value) {
        for (SemanticKeyActionDto action : values()) {
            if (action.jsonValue.equals(value)) {
                return action;
            }
        }
        throw new OpenAiIntegrationException("Unknown semanticKeyAction value: " + value);
    }
}

package com.gnd.publisher.dto.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gnd.publisher.exception.LlmIntegrationException;

public enum ClassificationRejectionReasonDto {
    EDITORIAL_RULE_EXCLUDED,
    NOT_PUBLISHABLE_CATEGORY,
    SOURCE_RUN_QUOTA_EXCEEDED,
    DUPLICATE_SEMANTIC_EVENT,
    LOW_CONFIDENCE,
    CLASSIFICATION_FAILED;

    @JsonValue
    public String jsonValue() {
        return name();
    }

    /**
     * Parsing is case-insensitive and tolerates surrounding whitespace, because smaller local
     * models vary the casing of enum values even under a constrained schema.
     */
    @JsonCreator
    public static ClassificationRejectionReasonDto fromJson(String value) {
        if (value != null) {
            String normalized = value.trim();
            for (ClassificationRejectionReasonDto reason : values()) {
                if (reason.name().equalsIgnoreCase(normalized)) {
                    return reason;
                }
            }
        }
        throw new LlmIntegrationException("Unknown rejectionReason value: " + value);
    }
}

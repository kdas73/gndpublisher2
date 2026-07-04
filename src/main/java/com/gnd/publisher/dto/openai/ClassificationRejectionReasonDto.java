package com.gnd.publisher.dto.openai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gnd.publisher.exception.OpenAiIntegrationException;

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

    @JsonCreator
    public static ClassificationRejectionReasonDto fromJson(String value) {
        for (ClassificationRejectionReasonDto reason : values()) {
            if (reason.name().equals(value)) {
                return reason;
            }
        }
        throw new OpenAiIntegrationException("Unknown rejectionReason value: " + value);
    }
}

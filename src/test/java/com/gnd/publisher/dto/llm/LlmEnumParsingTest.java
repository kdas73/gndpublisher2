package com.gnd.publisher.dto.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gnd.publisher.exception.LlmIntegrationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Smaller local models vary the casing of enum values even under a constrained schema, so parsing
 * must tolerate it rather than fail the whole classification.
 */
class LlmEnumParsingTest {

    @ParameterizedTest
    @ValueSource(strings = {"created_new", "CREATED_NEW", "Created_New", "  created_new  "})
    void parsesSemanticKeyActionRegardlessOfCasing(String value) {
        assertThat(SemanticKeyActionDto.fromJson(value)).isEqualTo(SemanticKeyActionDto.CREATED_NEW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"matched_existing", "MATCHED_EXISTING", "Matched_Existing"})
    void parsesMatchedExistingRegardlessOfCasing(String value) {
        assertThat(SemanticKeyActionDto.fromJson(value)).isEqualTo(SemanticKeyActionDto.MATCHED_EXISTING);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NOT_PUBLISHABLE_CATEGORY",
            "not_publishable_category",
            "Not_Publishable_Category",
            " not_publishable_category "})
    void parsesRejectionReasonRegardlessOfCasing(String value) {
        assertThat(ClassificationRejectionReasonDto.fromJson(value))
                .isEqualTo(ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY);
    }

    @Test
    void serializesUsingTheCanonicalWireValue() {
        assertThat(SemanticKeyActionDto.CREATED_NEW.jsonValue()).isEqualTo("created_new");
        assertThat(ClassificationRejectionReasonDto.LOW_CONFIDENCE.jsonValue()).isEqualTo("LOW_CONFIDENCE");
    }

    @Test
    void rejectsUnknownValues() {
        assertThatThrownBy(() -> SemanticKeyActionDto.fromJson("invented"))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("Unknown semanticKeyAction value");

        assertThatThrownBy(() -> ClassificationRejectionReasonDto.fromJson("invented"))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("Unknown rejectionReason value");
    }

    @Test
    void rejectsNullValues() {
        assertThatThrownBy(() -> SemanticKeyActionDto.fromJson(null))
                .isInstanceOf(LlmIntegrationException.class);

        assertThatThrownBy(() -> ClassificationRejectionReasonDto.fromJson(null))
                .isInstanceOf(LlmIntegrationException.class);
    }
}

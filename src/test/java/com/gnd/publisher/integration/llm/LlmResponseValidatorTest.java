package com.gnd.publisher.integration.llm;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryClassificationResponse;
import com.gnd.publisher.dto.llm.CategoryOptionDto;
import com.gnd.publisher.dto.llm.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.llm.LlmNewsItemDto;
import com.gnd.publisher.dto.llm.PublicationContentResponse;
import com.gnd.publisher.dto.llm.SemanticEventKeyCandidateDto;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.junit.jupiter.api.Test;

/**
 * Validation operates purely on DTOs, so it applies identically to every provider.
 */
class LlmResponseValidatorTest {

    private final LlmResponseValidator validator = new LlmResponseValidator();

    @Test
    void acceptsAWellFormedClassification() {
        assertThatCode(() -> validator.validateClassification(
                request(List.of("politics")),
                response("politics", List.of("politics"), SemanticKeyActionDto.MATCHED_EXISTING, "event-456",
                        0.91, true, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnconfiguredPrimaryCategory() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("politics")),
                response("sports", List.of("sports"), SemanticKeyActionDto.CREATED_NEW, null, 0.91, false,
                        ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void rejectsCategoryCodesWithoutThePrimaryCategory() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("politics")),
                response("politics", List.of("weather"), SemanticKeyActionDto.CREATED_NEW, null, 0.91, false,
                        ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("must include primaryCategoryCode");
    }

    @Test
    void rejectsMatchedExistingWithoutACandidateId() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("politics")),
                response("politics", List.of("politics"), SemanticKeyActionDto.MATCHED_EXISTING, null, 0.91,
                        true, null)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("matched_existing requires matchedSemanticEventId");
    }

    @Test
    void rejectsCreatedNewWithAMatchedId() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("politics")),
                response("politics", List.of("politics"), SemanticKeyActionDto.CREATED_NEW, "event-456", 0.91,
                        true, null)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("created_new requires matchedSemanticEventId to be null");
    }

    @Test
    void rejectsPublishingWithoutAPublishableCategory() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("weather")),
                response("politics", List.of("politics"), SemanticKeyActionDto.CREATED_NEW, null, 0.91, true,
                        null)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("shouldPublish cannot be true");
    }

    @Test
    void rejectsEditorialRuleExclusionThatStillPublishes() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("politics")),
                response("politics", List.of("politics"), SemanticKeyActionDto.CREATED_NEW, null, 0.91, true,
                        ClassificationRejectionReasonDto.EDITORIAL_RULE_EXCLUDED)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("EDITORIAL_RULE_EXCLUDED requires shouldPublish false");
    }

    @Test
    void rejectsConfidenceOutsideTheUnitRange() {
        assertThatThrownBy(() -> validator.validateClassification(
                request(List.of("politics")),
                response("politics", List.of("politics"), SemanticKeyActionDto.CREATED_NEW, null, 1.5, true,
                        null)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("confidence must be between 0.0 and 1.0");

        assertThatThrownBy(() -> validator.validatePublicationContent(
                new PublicationContentResponse("Title", "Summary", -0.1)))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("confidence must be between 0.0 and 1.0");
    }

    @Test
    void acceptsAWellFormedPublicationContentResponse() {
        assertThatCode(() -> validator.validatePublicationContent(
                new PublicationContentResponse("Title", "Summary", 0.9)))
                .doesNotThrowAnyException();
    }

    private CategoryClassificationRequest request(List<String> publishableCategoryCodes) {
        return new CategoryClassificationRequest(
                new LlmNewsItemDto(
                        "Greek parliament approves new migration bill",
                        "Greek lawmakers approved a new migration bill after a lengthy debate.",
                        "ERT News",
                        "https://example.gr/news/item",
                        Instant.parse("2026-07-01T10:15:00Z")),
                List.of(
                        new CategoryOptionDto("politics", "Government, elections, laws, public administration"),
                        new CategoryOptionDto("weather", "Weather alerts, climate events, natural hazards")),
                publishableCategoryCodes,
                List.of(),
                List.of(new SemanticEventKeyCandidateDto(
                        "event-456",
                        "greek parliament debates migration bill")));
    }

    private CategoryClassificationResponse response(
            String primaryCategoryCode,
            List<String> categoryCodes,
            SemanticKeyActionDto action,
            String matchedSemanticEventId,
            double confidence,
            boolean shouldPublish,
            ClassificationRejectionReasonDto rejectionReason) {
        return new CategoryClassificationResponse(
                primaryCategoryCode,
                categoryCodes,
                "greek parliament approves migration bill",
                action,
                matchedSemanticEventId,
                confidence,
                shouldPublish,
                rejectionReason);
    }
}

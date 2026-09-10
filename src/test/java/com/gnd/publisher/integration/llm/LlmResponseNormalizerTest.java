package com.gnd.publisher.integration.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryClassificationResponse;
import com.gnd.publisher.dto.llm.CategoryOptionDto;
import com.gnd.publisher.dto.llm.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.llm.LlmNewsItemDto;
import com.gnd.publisher.dto.llm.SemanticEventKeyCandidateDto;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;

import org.junit.jupiter.api.Test;

/**
 * Normalization operates purely on DTOs, so it is provider-independent by construction: whichever
 * provider produced the response, it passes through exactly these rules.
 */
class LlmResponseNormalizerTest {

    private final LlmResponseNormalizer normalizer = new LlmResponseNormalizer();

    @Test
    void clearsMatchedSemanticEventIdWhenActionCreatesNewEvent() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("politics")),
                response(
                        List.of("politics"),
                        SemanticKeyActionDto.CREATED_NEW,
                        "event-456",
                        true,
                        null));

        assertThat(response.semanticKeyAction()).isEqualTo(SemanticKeyActionDto.CREATED_NEW);
        assertThat(response.matchedSemanticEventId()).isNull();
    }

    @Test
    void fallsBackToCreatedNewWhenMatchedIdIsNotACandidate() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("politics")),
                response(
                        List.of("politics"),
                        SemanticKeyActionDto.MATCHED_EXISTING,
                        "event-does-not-exist",
                        true,
                        null));

        assertThat(response.semanticKeyAction()).isEqualTo(SemanticKeyActionDto.CREATED_NEW);
        assertThat(response.matchedSemanticEventId()).isNull();
    }

    @Test
    void keepsAValidMatchedSemanticEventId() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("politics")),
                response(
                        List.of("politics"),
                        SemanticKeyActionDto.MATCHED_EXISTING,
                        "event-456",
                        true,
                        null));

        assertThat(response.semanticKeyAction()).isEqualTo(SemanticKeyActionDto.MATCHED_EXISTING);
        assertThat(response.matchedSemanticEventId()).isEqualTo("event-456");
    }

    @Test
    void addsPrimaryCategoryToCategoryCodesWhenTheModelOmitsIt() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("politics")),
                response(List.of(), SemanticKeyActionDto.CREATED_NEW, null, true, null));

        assertThat(response.categoryCodes()).containsExactly("politics");
    }

    @Test
    void defaultsRejectionReasonWhenNoSelectedCategoryIsPublishable() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("weather")),
                response(List.of("politics"), SemanticKeyActionDto.CREATED_NEW, null, false, null));

        assertThat(response.categoryCodes()).containsExactly("politics");
        assertThat(response.rejectionReason())
                .isEqualTo(ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY);
    }

    @Test
    void disablesPublishingWhenTheModelPublishesWithoutAPublishableCategory() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("weather")),
                response(List.of("politics"), SemanticKeyActionDto.CREATED_NEW, null, true, null));

        assertThat(response.shouldPublish()).isFalse();
        assertThat(response.rejectionReason())
                .isEqualTo(ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY);
    }

    @Test
    void keepsAModelSuppliedRejectionReason() {
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(
                request(List.of("politics")),
                response(
                        List.of("politics"),
                        SemanticKeyActionDto.CREATED_NEW,
                        null,
                        false,
                        ClassificationRejectionReasonDto.EDITORIAL_RULE_EXCLUDED));

        assertThat(response.rejectionReason())
                .isEqualTo(ClassificationRejectionReasonDto.EDITORIAL_RULE_EXCLUDED);
    }

    @Test
    void returnsTheSameInstanceWhenNothingNeedsRepair() {
        CategoryClassificationResponse original = response(
                List.of("politics"),
                SemanticKeyActionDto.MATCHED_EXISTING,
                "event-456",
                true,
                null);

        assertThat(normalizer.normalizeClassificationResponse(request(List.of("politics")), original))
                .isSameAs(original);
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
                List.of("For sports news, publish only items with concrete match results."),
                List.of(new SemanticEventKeyCandidateDto(
                        "event-456",
                        "greek parliament debates migration bill")));
    }

    private CategoryClassificationResponse response(
            List<String> categoryCodes,
            SemanticKeyActionDto action,
            String matchedSemanticEventId,
            boolean shouldPublish,
            ClassificationRejectionReasonDto rejectionReason) {
        return new CategoryClassificationResponse(
                "politics",
                categoryCodes,
                "greek parliament approves migration bill",
                action,
                matchedSemanticEventId,
                0.91,
                shouldPublish,
                rejectionReason);
    }
}

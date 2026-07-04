package com.gnd.publisher.integration.openai;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.openai.SemanticKeyActionDto;
import com.gnd.publisher.dto.openai.SummaryResponse;
import com.gnd.publisher.dto.openai.TranslationResponse;
import com.gnd.publisher.exception.OpenAiIntegrationException;

import org.springframework.stereotype.Component;

@Component
public class OpenAiResponseValidator {

    public void validateClassification(
            CategoryClassificationRequest request,
            CategoryClassificationResponse response) {
        requireConfidence(response.confidence(), "classification");

        Set<String> allowedCategoryCodes = request.categoryOptions().stream()
                .map(option -> option.code())
                .collect(Collectors.toUnmodifiableSet());
        if (!allowedCategoryCodes.contains(response.primaryCategoryCode())) {
            throw new OpenAiIntegrationException("Classification primaryCategoryCode is not configured: "
                    + response.primaryCategoryCode());
        }
        if (response.categoryCodes().stream().anyMatch(code -> !allowedCategoryCodes.contains(code))) {
            throw new OpenAiIntegrationException("Classification categoryCodes contain an unconfigured category");
        }
        if (!response.categoryCodes().contains(response.primaryCategoryCode())) {
            throw new OpenAiIntegrationException("Classification categoryCodes must include primaryCategoryCode");
        }

        Set<String> candidateIds = request.candidateSemanticEvents().stream()
                .map(candidate -> candidate.id())
                .collect(Collectors.toUnmodifiableSet());
        if (response.semanticKeyAction() == SemanticKeyActionDto.MATCHED_EXISTING) {
            if (response.matchedSemanticEventId() == null || response.matchedSemanticEventId().isBlank()) {
                throw new OpenAiIntegrationException("matched_existing requires matchedSemanticEventId");
            }
            if (!candidateIds.contains(response.matchedSemanticEventId())) {
                throw new OpenAiIntegrationException("matchedSemanticEventId was not present in candidates");
            }
        }
        if (response.semanticKeyAction() == SemanticKeyActionDto.CREATED_NEW
                && response.matchedSemanticEventId() != null) {
            throw new OpenAiIntegrationException("created_new requires matchedSemanticEventId to be null");
        }

        boolean hasPublishableCategory = new HashSet<>(request.publishableCategoryCodes())
                .removeAll(response.categoryCodes());
        if (response.shouldPublish() && !hasPublishableCategory) {
            throw new OpenAiIntegrationException("shouldPublish cannot be true without a publishable category");
        }
        if (!response.shouldPublish()
                && response.rejectionReason() == null
                && !hasPublishableCategory) {
            throw new OpenAiIntegrationException("Non-publishable classification requires a rejectionReason");
        }
        if (response.rejectionReason() == ClassificationRejectionReasonDto.EDITORIAL_RULE_EXCLUDED
                && response.shouldPublish()) {
            throw new OpenAiIntegrationException("EDITORIAL_RULE_EXCLUDED requires shouldPublish false");
        }
    }

    public void validateSummary(SummaryResponse response) {
        requireConfidence(response.confidence(), "summary");
    }

    public void validateTranslation(TranslationResponse response) {
        requireConfidence(response.confidence(), "translation");
    }

    private void requireConfidence(double confidence, String responseType) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new OpenAiIntegrationException(responseType + " confidence must be between 0.0 and 1.0");
        }
    }
}

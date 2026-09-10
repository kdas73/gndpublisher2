package com.gnd.publisher.integration.llm;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryClassificationResponse;
import com.gnd.publisher.dto.llm.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.llm.PublicationContentResponse;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.springframework.stereotype.Component;

@Component
public class LlmResponseValidator {

    public void validateClassification(
            CategoryClassificationRequest request,
            CategoryClassificationResponse response) {
        requireConfidence(response.confidence(), "classification");

        Set<String> allowedCategoryCodes = request.categoryOptions().stream()
                .map(option -> option.code())
                .collect(Collectors.toUnmodifiableSet());
        if (!allowedCategoryCodes.contains(response.primaryCategoryCode())) {
            throw new LlmIntegrationException("Classification primaryCategoryCode is not configured: "
                    + response.primaryCategoryCode());
        }
        if (response.categoryCodes().stream().anyMatch(code -> !allowedCategoryCodes.contains(code))) {
            throw new LlmIntegrationException("Classification categoryCodes contain an unconfigured category");
        }
        if (!response.categoryCodes().contains(response.primaryCategoryCode())) {
            throw new LlmIntegrationException("Classification categoryCodes must include primaryCategoryCode");
        }

        Set<String> candidateIds = request.candidateSemanticEvents().stream()
                .map(candidate -> candidate.id())
                .collect(Collectors.toUnmodifiableSet());
        if (response.semanticKeyAction() == SemanticKeyActionDto.MATCHED_EXISTING) {
            if (response.matchedSemanticEventId() == null || response.matchedSemanticEventId().isBlank()) {
                throw new LlmIntegrationException("matched_existing requires matchedSemanticEventId");
            }
            if (!candidateIds.contains(response.matchedSemanticEventId())) {
                throw new LlmIntegrationException("matchedSemanticEventId was not present in candidates");
            }
        }
        if (response.semanticKeyAction() == SemanticKeyActionDto.CREATED_NEW
                && response.matchedSemanticEventId() != null) {
            throw new LlmIntegrationException("created_new requires matchedSemanticEventId to be null");
        }

        boolean hasPublishableCategory = new HashSet<>(request.publishableCategoryCodes())
                .removeAll(response.categoryCodes());
        if (response.shouldPublish() && !hasPublishableCategory) {
            throw new LlmIntegrationException("shouldPublish cannot be true without a publishable category");
        }
        if (!response.shouldPublish()
                && response.rejectionReason() == null
                && !hasPublishableCategory) {
            throw new LlmIntegrationException("Non-publishable classification requires a rejectionReason");
        }
        if (response.rejectionReason() == ClassificationRejectionReasonDto.EDITORIAL_RULE_EXCLUDED
                && response.shouldPublish()) {
            throw new LlmIntegrationException("EDITORIAL_RULE_EXCLUDED requires shouldPublish false");
        }
    }

    public void validatePublicationContent(PublicationContentResponse response) {
        requireConfidence(response.confidence(), "publication content");
    }

    private void requireConfidence(double confidence, String responseType) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new LlmIntegrationException(responseType + " confidence must be between 0.0 and 1.0");
        }
    }
}

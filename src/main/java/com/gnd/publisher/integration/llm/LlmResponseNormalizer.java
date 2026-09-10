package com.gnd.publisher.integration.llm;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryClassificationResponse;
import com.gnd.publisher.dto.llm.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Repairs the recoverable inconsistencies a model can produce, before validation rejects them.
 * Provider-agnostic on purpose: every provider goes through exactly these rules.
 */
@Component
public class LlmResponseNormalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmResponseNormalizer.class);

    public CategoryClassificationResponse normalizeClassificationResponse(
            CategoryClassificationRequest request,
            CategoryClassificationResponse response) {
        List<String> categoryCodes = normalizedCategoryCodes(response);
        String matchedSemanticEventId = response.matchedSemanticEventId();
        SemanticKeyActionDto semanticKeyAction = response.semanticKeyAction();
        if (semanticKeyAction == SemanticKeyActionDto.CREATED_NEW) {
            matchedSemanticEventId = null;
        }
        if (semanticKeyAction == SemanticKeyActionDto.MATCHED_EXISTING
                && !isValidCandidateId(request, matchedSemanticEventId)) {
            LOGGER.warn("LLM returned matched_existing without a valid matchedSemanticEventId; "
                    + "falling back to created_new. matchedSemanticEventId={}", matchedSemanticEventId);
            semanticKeyAction = SemanticKeyActionDto.CREATED_NEW;
            matchedSemanticEventId = null;
        }
        boolean shouldPublish = normalizedShouldPublish(request, response, categoryCodes);
        ClassificationRejectionReasonDto rejectionReason = normalizedRejectionReason(request, response, categoryCodes);

        if (!categoryCodes.equals(response.categoryCodes())
                || semanticKeyAction != response.semanticKeyAction()
                || !Objects.equals(matchedSemanticEventId, response.matchedSemanticEventId())
                || shouldPublish != response.shouldPublish()
                || rejectionReason != response.rejectionReason()) {
            return new CategoryClassificationResponse(
                    response.primaryCategoryCode(),
                    categoryCodes,
                    response.semanticKey(),
                    semanticKeyAction,
                    matchedSemanticEventId,
                    response.confidence(),
                    shouldPublish,
                    rejectionReason);
        }
        return response;
    }

    private boolean isValidCandidateId(CategoryClassificationRequest request, String matchedSemanticEventId) {
        if (matchedSemanticEventId == null || matchedSemanticEventId.isBlank()) {
            return false;
        }
        return request.candidateSemanticEvents().stream()
                .anyMatch(candidate -> candidate.id().equals(matchedSemanticEventId));
    }

    private List<String> normalizedCategoryCodes(CategoryClassificationResponse response) {
        Set<String> categoryCodes = new LinkedHashSet<>();
        if (response.categoryCodes() != null) {
            categoryCodes.addAll(response.categoryCodes());
        }
        if (response.primaryCategoryCode() != null && !response.primaryCategoryCode().isBlank()) {
            categoryCodes.add(response.primaryCategoryCode());
        }
        return List.copyOf(categoryCodes);
    }

    private ClassificationRejectionReasonDto normalizedRejectionReason(
            CategoryClassificationRequest request,
            CategoryClassificationResponse response,
            List<String> categoryCodes) {
        if (response.rejectionReason() != null) {
            return response.rejectionReason();
        }
        Set<String> publishableCodes = Set.copyOf(request.publishableCategoryCodes());
        boolean hasPublishableCategory = categoryCodes.stream().anyMatch(publishableCodes::contains);
        if (!hasPublishableCategory) {
            return ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY;
        }
        return null;
    }

    private boolean normalizedShouldPublish(
            CategoryClassificationRequest request,
            CategoryClassificationResponse response,
            List<String> categoryCodes) {
        if (!response.shouldPublish()) {
            return false;
        }
        Set<String> publishableCodes = Set.copyOf(request.publishableCategoryCodes());
        return categoryCodes.stream().anyMatch(publishableCodes::contains);
    }
}

package com.gnd.publisher.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gnd.publisher.config.PublishingProperties;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.SourceQuotaCandidate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceQuotaSelectionService {

    private final NewsItemRepository newsItemRepository;
    private final PublishingProperties publishingProperties;

    public SourceQuotaSelectionService(
            NewsItemRepository newsItemRepository,
            PublishingProperties publishingProperties) {
        this.newsItemRepository = newsItemRepository;
        this.publishingProperties = publishingProperties;
    }

    @Transactional
    public List<NewsItem> selectForProcessingRun(String processingRunId) {
        List<SourceQuotaCandidate> candidates = newsItemRepository.findSourceQuotaCandidates(processingRunId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<String, List<SourceQuotaCandidate>> candidatesBySourceCode = candidates.stream()
                .collect(Collectors.groupingBy(
                        SourceQuotaCandidate::sourceCode,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<NewsItem> changedItems = new ArrayList<>();
        List<NewsItem> selectedItems = new ArrayList<>();
        candidatesBySourceCode.forEach((sourceCode, sourceCandidates) -> {
            int quota = quotaFor(sourceCode);
            List<SourceQuotaCandidate> rankedCandidates = sourceCandidates.stream()
                    .sorted(candidateComparator(processingRunId))
                    .toList();

            for (int index = 0; index < rankedCandidates.size(); index++) {
                NewsItem newsItem = rankedCandidates.get(index).newsItem();
                newsItem.assignProcessingRun(processingRunId);
                if (index < quota) {
                    newsItem.markSelectedForPublication();
                    selectedItems.add(newsItem);
                } else {
                    newsItem.markRejectedBySourceQuota();
                }
                changedItems.add(newsItem);
            }
        });

        newsItemRepository.saveAll(changedItems);
        return selectedItems;
    }

    private int quotaFor(String sourceCode) {
        return Optional.ofNullable(publishingProperties.sourceQuotaOverrides())
                .map(overrides -> overrides.get(sourceCode))
                .orElse(publishingProperties.defaultMaxItemsPerSourcePerRun());
    }

    private static Comparator<SourceQuotaCandidate> candidateComparator(String processingRunId) {
        return Comparator
                .comparing(
                        (SourceQuotaCandidate candidate) -> isCurrentRun(candidate.newsItem(), processingRunId),
                        Comparator.reverseOrder())
                .thenComparingInt(SourceQuotaCandidate::publicationPriority)
                .thenComparing(
                        candidate -> publicationTimestamp(candidate.newsItem()),
                        Comparator.reverseOrder())
                .thenComparing(
                        candidate -> Optional.ofNullable(candidate.newsItem().getClassificationConfidence())
                                .orElse(BigDecimal.ZERO),
                        Comparator.reverseOrder());
    }

    private static boolean isCurrentRun(NewsItem newsItem, String processingRunId) {
        return processingRunId.equals(newsItem.getProcessingRunId());
    }

    private static Instant publicationTimestamp(NewsItem newsItem) {
        return Optional.ofNullable(newsItem.getPublishedAt()).orElse(newsItem.getFetchedAt());
    }
}

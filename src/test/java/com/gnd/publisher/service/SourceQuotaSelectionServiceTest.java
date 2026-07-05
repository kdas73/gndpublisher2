package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gnd.publisher.config.PublishingProperties;
import com.gnd.publisher.domain.enums.RejectionReason;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.SourceQuotaCandidate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceQuotaSelectionServiceTest {

    private static final String RUN_ID = "ingestion-2026-07-01T12:00:00Z-test";
    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private NewsItemRepository newsItemRepository;

    @Test
    void selectsDefaultQuotaPerSource() {
        RssSource sourceA = source("source-a");
        RssSource sourceB = source("source-b");
        NewsItem firstA = candidate(sourceA, "First A", NOW.minusSeconds(10), "0.7");
        NewsItem secondA = candidate(sourceA, "Second A", NOW.minusSeconds(20), "0.7");
        NewsItem overflowA = candidate(sourceA, "Overflow A", NOW.minusSeconds(30), "0.7");
        NewsItem onlyB = candidate(sourceB, "Only B", NOW.minusSeconds(40), "0.7");
        when(newsItemRepository.findSourceQuotaCandidates(RUN_ID)).thenReturn(List.of(
                new SourceQuotaCandidate(firstA, sourceA.getCode(), 10),
                new SourceQuotaCandidate(secondA, sourceA.getCode(), 10),
                new SourceQuotaCandidate(overflowA, sourceA.getCode(), 10),
                new SourceQuotaCandidate(onlyB, sourceB.getCode(), 10)));

        List<NewsItem> selected = service(defaultQuota(2, Map.of())).selectForProcessingRun(RUN_ID);

        assertThat(selected).containsExactly(firstA, secondA, onlyB);
        assertThat(firstA.isSelectedForPublication()).isTrue();
        assertThat(secondA.isSelectedForPublication()).isTrue();
        assertThat(onlyB.isSelectedForPublication()).isTrue();
        assertThat(overflowA.isSelectedForPublication()).isFalse();
        assertThat(overflowA.getRejectionReason()).isEqualTo(RejectionReason.SOURCE_RUN_QUOTA_EXCEEDED);
        verifySaved(firstA, secondA, overflowA, onlyB);
    }

    @Test
    void appliesPerSourceOverrideBySourceCode() {
        RssSource source = source("source-a");
        NewsItem selectedItem = candidate(source, "Selected", NOW, "0.7");
        NewsItem overflowItem = candidate(source, "Overflow", NOW.minusSeconds(1), "0.7");
        when(newsItemRepository.findSourceQuotaCandidates(RUN_ID)).thenReturn(List.of(
                new SourceQuotaCandidate(selectedItem, source.getCode(), 10),
                new SourceQuotaCandidate(overflowItem, source.getCode(), 10)));

        List<NewsItem> selected = service(defaultQuota(3, Map.of("source-a", 1))).selectForProcessingRun(RUN_ID);

        assertThat(selected).containsExactly(selectedItem);
        assertThat(selectedItem.isSelectedForPublication()).isTrue();
        assertThat(overflowItem.isSelectedForPublication()).isFalse();
        assertThat(overflowItem.getRejectionReason()).isEqualTo(RejectionReason.SOURCE_RUN_QUOTA_EXCEEDED);
        verifySaved(selectedItem, overflowItem);
    }

    @Test
    void sortsByPriorityPublishedAtAndConfidence() {
        RssSource source = source("source-a");
        NewsItem lowerPriority = candidate(source, "Lower priority wins", NOW.minusSeconds(100), "0.1");
        NewsItem newer = candidate(source, "Newer wins", NOW.minusSeconds(10), "0.1");
        NewsItem higherConfidence = candidate(source, "Higher confidence wins", NOW.minusSeconds(20), "0.9");
        NewsItem lowerConfidence = candidate(source, "Lower confidence loses", NOW.minusSeconds(20), "0.3");
        when(newsItemRepository.findSourceQuotaCandidates(RUN_ID)).thenReturn(List.of(
                new SourceQuotaCandidate(lowerConfidence, source.getCode(), 10),
                new SourceQuotaCandidate(higherConfidence, source.getCode(), 10),
                new SourceQuotaCandidate(newer, source.getCode(), 10),
                new SourceQuotaCandidate(lowerPriority, source.getCode(), 5)));

        List<NewsItem> selected = service(defaultQuota(3, Map.of())).selectForProcessingRun(RUN_ID);

        assertThat(selected).containsExactly(lowerPriority, newer, higherConfidence);
        assertThat(lowerConfidence.isSelectedForPublication()).isFalse();
        assertThat(lowerConfidence.getRejectionReason()).isEqualTo(RejectionReason.SOURCE_RUN_QUOTA_EXCEEDED);
        verifySaved(lowerPriority, newer, higherConfidence, lowerConfidence);
    }

    @Test
    void reconsidersItemsPreviouslyRejectedOnlyBySourceQuota() {
        RssSource source = source("source-a");
        NewsItem backlogItem = candidate(
                source,
                "Backlog item",
                NOW.minusSeconds(60),
                "0.8",
                "ingestion-2026-07-01T09:00:00Z-old");
        backlogItem.markRejectedBySourceQuota();
        when(newsItemRepository.findSourceQuotaCandidates(RUN_ID)).thenReturn(List.of(
                new SourceQuotaCandidate(backlogItem, source.getCode(), 10)));

        List<NewsItem> selected = service(defaultQuota(1, Map.of())).selectForProcessingRun(RUN_ID);

        assertThat(selected).containsExactly(backlogItem);
        assertThat(backlogItem.isSelectedForPublication()).isTrue();
        assertThat(backlogItem.getRejectionReason()).isNull();
        assertThat(backlogItem.getProcessingRunId()).isEqualTo(RUN_ID);
        verifySaved(backlogItem);
    }

    @Test
    void prioritizesCurrentRunItemsBeforeQuotaBacklog() {
        RssSource source = source("source-a");
        NewsItem currentRunItem = candidate(source, "Current run item", NOW.minusSeconds(300), "0.4");
        NewsItem backlogItem = candidate(
                source,
                "Backlog item",
                NOW,
                "0.9",
                "ingestion-2026-07-01T09:00:00Z-old");
        backlogItem.markRejectedBySourceQuota();
        when(newsItemRepository.findSourceQuotaCandidates(RUN_ID)).thenReturn(List.of(
                new SourceQuotaCandidate(backlogItem, source.getCode(), 1),
                new SourceQuotaCandidate(currentRunItem, source.getCode(), 10)));

        List<NewsItem> selected = service(defaultQuota(1, Map.of())).selectForProcessingRun(RUN_ID);

        assertThat(selected).containsExactly(currentRunItem);
        assertThat(currentRunItem.isSelectedForPublication()).isTrue();
        assertThat(backlogItem.isSelectedForPublication()).isFalse();
        assertThat(backlogItem.getRejectionReason()).isEqualTo(RejectionReason.SOURCE_RUN_QUOTA_EXCEEDED);
        assertThat(backlogItem.getProcessingRunId()).isEqualTo(RUN_ID);
        verifySaved(currentRunItem, backlogItem);
    }

    @Test
    void skipsSaveWhenNoCandidatesExist() {
        when(newsItemRepository.findSourceQuotaCandidates(RUN_ID)).thenReturn(List.of());

        List<NewsItem> selected = service(defaultQuota(2, Map.of())).selectForProcessingRun(RUN_ID);

        assertThat(selected).isEmpty();
        verify(newsItemRepository, never()).saveAll(any());
    }

    private SourceQuotaSelectionService service(PublishingProperties properties) {
        return new SourceQuotaSelectionService(newsItemRepository, properties);
    }

    private PublishingProperties defaultQuota(int defaultQuota, Map<String, Integer> overrides) {
        return new PublishingProperties(
                Set.of("en"),
                defaultQuota,
                overrides,
                600,
                3500,
                new PublishingProperties.ImportantNewsDigest(true, 2, "important-news"));
    }

    private RssSource source(String code) {
        return RssSource.create(code, code, "https://feeds.example.test/" + code, "el", true);
    }

    private NewsItem candidate(RssSource source, String title, Instant publishedAt, String confidence) {
        return candidate(source, title, publishedAt, confidence, RUN_ID);
    }

    private NewsItem candidate(
            RssSource source,
            String title,
            Instant publishedAt,
            String confidence,
            String processingRunId) {
        NewsItem newsItem = NewsItem.fromRssFeedItem(
                source,
                new RssFeedItemDto(
                        Optional.empty(),
                        title,
                        Optional.of("https://example.test/" + title.toLowerCase().replace(" ", "-")),
                        Optional.empty(),
                        Optional.of(publishedAt)),
                NOW);
        Category category = Category.create("politics", "Politics", "Government", true, true, 10);
        SemanticNewsEvent event = SemanticNewsEvent.create(title.toLowerCase(), category, NOW);
        newsItem.assignProcessingRun(processingRunId);
        newsItem.markClassified(event, new BigDecimal(confidence), true, null, NOW);
        return newsItem;
    }

    private void verifySaved(NewsItem... expectedItems) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NewsItem>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(newsItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(expectedItems);
    }
}

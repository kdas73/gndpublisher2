package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.gnd.publisher.domain.enums.ClassificationStatus;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.integration.rss.RssClient;
import com.gnd.publisher.integration.rss.RssFeedParser;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.RssSourceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedIngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private RssSourceRepository rssSourceRepository;

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private SourceDeduplicationService sourceDeduplicationService;

    @Mock
    private CategorizationService categorizationService;

    @Mock
    private RssClient rssClient;

    @Mock
    private RssFeedParser rssFeedParser;

    @Test
    void ingestsEnabledSourcesIntoPendingNewsItems() {
        RssSource source = RssSource.create("Kathimerini", "https://feeds.example.test/kathimerini", "el", true);
        RssFeedItemDto feedItem = new RssFeedItemDto(
                Optional.of("external-1"),
                "Fixture title",
                Optional.of("https://example.test/news/fixture"),
                Optional.of("Fixture summary"),
                Optional.of(Instant.parse("2026-07-01T10:00:00Z")));
        when(rssSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(rssClient.fetch(URI.create(source.getUrl()))).thenReturn("<rss />");
        when(rssFeedParser.parse("<rss />")).thenReturn(List.of(feedItem));
        NewsItem deduplicatedNewsItem = NewsItem.fromRssFeedItem(source, feedItem, NOW);
        when(sourceDeduplicationService.newItemsForSource(source, List.of(feedItem), NOW))
                .thenReturn(List.of(deduplicatedNewsItem));
        when(newsItemRepository.saveAll(List.of(deduplicatedNewsItem))).thenReturn(List.of(deduplicatedNewsItem));

        service().ingestEnabledSources();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NewsItem>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(newsItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(newsItem -> {
            assertThat(newsItem.getSource()).isSameAs(source);
            assertThat(newsItem.getSourceUrl()).isEqualTo("https://example.test/news/fixture");
            assertThat(newsItem.getExternalId()).isEqualTo("external-1");
            assertThat(newsItem.getTitle()).isEqualTo("Fixture title");
            assertThat(newsItem.getSummary()).isEqualTo("Fixture summary");
            assertThat(newsItem.getPublishedAt()).isEqualTo("2026-07-01T10:00:00Z");
            assertThat(newsItem.getFetchedAt()).isEqualTo(NOW);
            assertThat(newsItem.getOriginalLanguage()).isEqualTo("el");
            assertThat(newsItem.getClassificationStatus()).isEqualTo(ClassificationStatus.PENDING);
            assertThat(newsItem.isPublicationCandidate()).isFalse();
            assertThat(newsItem.isSelectedForPublication()).isFalse();
            assertThat(newsItem.getRejectionReason()).isNull();
        });
        verify(categorizationService).classifyNewItems(List.of(deduplicatedNewsItem));
    }

    @Test
    void doesNotSaveWhenAllSourceItemsAreDuplicates() {
        RssSource source = RssSource.create("Kathimerini", "https://feeds.example.test/kathimerini", "el", true);
        RssFeedItemDto feedItem = new RssFeedItemDto(
                Optional.of("external-1"),
                "Fixture title",
                Optional.of("https://example.test/news/fixture"),
                Optional.empty(),
                Optional.empty());
        when(rssSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(rssClient.fetch(URI.create(source.getUrl()))).thenReturn("<rss />");
        when(rssFeedParser.parse("<rss />")).thenReturn(List.of(feedItem));
        when(sourceDeduplicationService.newItemsForSource(source, List.of(feedItem), NOW)).thenReturn(List.of());

        service().ingestEnabledSources();

        verify(newsItemRepository, never()).saveAll(any());
        verify(categorizationService, never()).classifyNewItems(any());
    }

    @Test
    void continuesWithOtherSourcesWhenOneSourceFails() {
        RssSource failingSource = RssSource.create("Broken", "https://feeds.example.test/broken", "el", true);
        RssSource healthySource = RssSource.create("Healthy", "https://feeds.example.test/healthy", "el", true);
        when(rssSourceRepository.findByEnabledTrue()).thenReturn(List.of(failingSource, healthySource));
        when(rssClient.fetch(URI.create(failingSource.getUrl()))).thenThrow(new IllegalStateException("boom"));
        when(rssClient.fetch(URI.create(healthySource.getUrl()))).thenReturn("<rss />");
        RssFeedItemDto healthyFeedItem = new RssFeedItemDto(
                Optional.empty(),
                "Healthy item",
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        when(rssFeedParser.parse("<rss />")).thenReturn(List.of(healthyFeedItem));
        NewsItem healthyNewsItem = NewsItem.fromRssFeedItem(healthySource, healthyFeedItem, NOW);
        when(sourceDeduplicationService.newItemsForSource(healthySource, List.of(healthyFeedItem), NOW))
                .thenReturn(List.of(healthyNewsItem));
        when(newsItemRepository.saveAll(List.of(healthyNewsItem))).thenReturn(List.of(healthyNewsItem));

        service().ingestEnabledSources();

        verify(rssClient).fetch(URI.create(failingSource.getUrl()));
        verify(rssClient).fetch(URI.create(healthySource.getUrl()));
        verify(newsItemRepository).saveAll(any());
        verify(categorizationService).classifyNewItems(List.of(healthyNewsItem));
    }

    @Test
    void doesNotSaveWhenNoSourcesAreEnabled() {
        when(rssSourceRepository.findByEnabledTrue()).thenReturn(List.of());

        service().ingestEnabledSources();

        verify(newsItemRepository, never()).saveAll(any());
        verify(categorizationService, never()).classifyNewItems(any());
    }

    private FeedIngestionService service() {
        return new FeedIngestionService(
                rssSourceRepository,
                newsItemRepository,
                sourceDeduplicationService,
                categorizationService,
                rssClient,
                rssFeedParser,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}

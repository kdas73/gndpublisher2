package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.util.UrlNormalizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceDeduplicationServiceTest {

    private static final Instant FETCHED_AT = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private NewsItemRepository newsItemRepository;

    @Test
    void skipsExistingExternalIdDuplicate() {
        RssSource source = source("Source A");
        RssFeedItemDto feedItem = feedItem(Optional.of(" external-1 "), Optional.of("https://example.test/news/one"));
        when(newsItemRepository.existsBySourceAndExternalId(source, "external-1")).thenReturn(true);

        List<NewsItem> newsItems = service().newItemsForSource(source, List.of(feedItem), FETCHED_AT);

        assertThat(newsItems).isEmpty();
        verify(newsItemRepository, never()).existsBySourceAndSourceUrl(source, "https://example.test/news/one");
    }

    @Test
    void usesExternalIdBeforeUrlFallback() {
        RssSource source = source("Source A");
        RssFeedItemDto feedItem = feedItem(Optional.of("external-1"), Optional.of("https://example.test/news/one"));
        when(newsItemRepository.existsBySourceAndExternalId(source, "external-1")).thenReturn(false);

        List<NewsItem> newsItems = service().newItemsForSource(source, List.of(feedItem), FETCHED_AT);

        assertThat(newsItems).singleElement().satisfies(newsItem -> {
            assertThat(newsItem.getExternalId()).isEqualTo("external-1");
            assertThat(newsItem.getSourceUrl()).isEqualTo("https://example.test/news/one");
        });
        verify(newsItemRepository, never()).existsBySourceAndSourceUrl(source, "https://example.test/news/one");
    }

    @Test
    void skipsExistingUrlDuplicateWhenExternalIdIsMissing() {
        RssSource source = source("Source A");
        RssFeedItemDto feedItem = feedItem(Optional.empty(), Optional.of("https://example.test/news/one?utm_source=rss"));
        when(newsItemRepository.existsBySourceAndSourceUrl(source, "https://example.test/news/one")).thenReturn(true);

        List<NewsItem> newsItems = service().newItemsForSource(source, List.of(feedItem), FETCHED_AT);

        assertThat(newsItems).isEmpty();
    }

    @Test
    void sameUrlFromDifferentSourceIsNotSkippedByServiceState() {
        RssSource sourceA = source("Source A");
        RssSource sourceB = source("Source B");
        RssFeedItemDto feedItem = feedItem(Optional.empty(), Optional.of("https://example.test/news/one"));
        when(newsItemRepository.existsBySourceAndSourceUrl(sourceA, "https://example.test/news/one")).thenReturn(true);
        when(newsItemRepository.existsBySourceAndSourceUrl(sourceB, "https://example.test/news/one")).thenReturn(false);

        List<NewsItem> sourceANewsItems = service().newItemsForSource(sourceA, List.of(feedItem), FETCHED_AT);
        List<NewsItem> sourceBNewsItems = service().newItemsForSource(sourceB, List.of(feedItem), FETCHED_AT);

        assertThat(sourceANewsItems).isEmpty();
        assertThat(sourceBNewsItems).singleElement().satisfies(newsItem -> assertThat(newsItem.getSource()).isSameAs(sourceB));
    }

    @Test
    void skipsDuplicateEntriesWithinSameParsedBatch() {
        RssSource source = source("Source A");
        RssFeedItemDto first = feedItem(Optional.empty(), Optional.of("https://example.test/news/one?b=2&a=1"));
        RssFeedItemDto second = feedItem(Optional.empty(), Optional.of("https://example.test/news/one?a=1&b=2"));
        when(newsItemRepository.existsBySourceAndSourceUrl(source, "https://example.test/news/one?a=1&b=2"))
                .thenReturn(false);

        List<NewsItem> newsItems = service().newItemsForSource(source, List.of(first, second), FETCHED_AT);

        assertThat(newsItems).singleElement().satisfies(newsItem ->
                assertThat(newsItem.getSourceUrl()).isEqualTo("https://example.test/news/one?a=1&b=2"));
    }

    @Test
    void usesNormalizedSourceFeedUrlWhenItemLinkIsMissing() {
        RssSource source = source("Source A");
        RssFeedItemDto feedItem = feedItem(Optional.empty(), Optional.empty());
        when(newsItemRepository.existsBySourceAndSourceUrl(source, "https://feeds.example.test/source")).thenReturn(false);

        List<NewsItem> newsItems = service().newItemsForSource(source, List.of(feedItem), FETCHED_AT);

        assertThat(newsItems).singleElement().satisfies(newsItem ->
                assertThat(newsItem.getSourceUrl()).isEqualTo("https://feeds.example.test/source"));
    }

    private SourceDeduplicationService service() {
        return new SourceDeduplicationService(newsItemRepository, new UrlNormalizer());
    }

    private RssSource source(String name) {
        return RssSource.create(name, "https://feeds.example.test/source", "el", true);
    }

    private RssFeedItemDto feedItem(Optional<String> externalId, Optional<String> link) {
        return new RssFeedItemDto(externalId, "Fixture title", link, Optional.empty(), Optional.empty());
    }
}

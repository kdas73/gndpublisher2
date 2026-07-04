package com.gnd.publisher.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.util.UrlNormalizer;

import org.springframework.stereotype.Service;

@Service
public class SourceDeduplicationService {

    private final NewsItemRepository newsItemRepository;
    private final UrlNormalizer urlNormalizer;

    public SourceDeduplicationService(NewsItemRepository newsItemRepository, UrlNormalizer urlNormalizer) {
        this.newsItemRepository = newsItemRepository;
        this.urlNormalizer = urlNormalizer;
    }

    public List<NewsItem> newItemsForSource(RssSource source, List<RssFeedItemDto> feedItems, Instant fetchedAt) {
        Set<String> externalIdsSeenInBatch = new HashSet<>();
        Set<String> sourceUrlsSeenInBatch = new HashSet<>();

        return feedItems.stream()
                .flatMap(feedItem -> newItemForSource(
                        source,
                        feedItem,
                        fetchedAt,
                        externalIdsSeenInBatch,
                        sourceUrlsSeenInBatch).stream())
                .toList();
    }

    private Optional<NewsItem> newItemForSource(
            RssSource source,
            RssFeedItemDto feedItem,
            Instant fetchedAt,
            Set<String> externalIdsSeenInBatch,
            Set<String> sourceUrlsSeenInBatch) {
        String externalId = normalizeExternalId(feedItem);
        String sourceUrl = normalizeSourceUrl(source, feedItem);

        if (externalId != null) {
            if (!externalIdsSeenInBatch.add(externalId)
                    || newsItemRepository.existsBySourceAndExternalId(source, externalId)) {
                return Optional.empty();
            }
            return Optional.of(NewsItem.fromRssFeedItem(source, feedItem, fetchedAt, sourceUrl, externalId));
        }

        if (!sourceUrlsSeenInBatch.add(sourceUrl) || newsItemRepository.existsBySourceAndSourceUrl(source, sourceUrl)) {
            return Optional.empty();
        }
        return Optional.of(NewsItem.fromRssFeedItem(source, feedItem, fetchedAt, sourceUrl, null));
    }

    private String normalizeExternalId(RssFeedItemDto feedItem) {
        return feedItem.externalId()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(null);
    }

    private String normalizeSourceUrl(RssSource source, RssFeedItemDto feedItem) {
        String sourceUrl = feedItem.link().orElse(source.getUrl());
        return urlNormalizer.normalize(sourceUrl);
    }
}

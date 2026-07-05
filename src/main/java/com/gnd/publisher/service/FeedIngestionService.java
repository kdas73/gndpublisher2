package com.gnd.publisher.service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.integration.rss.RssClient;
import com.gnd.publisher.integration.rss.RssFeedParser;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.RssSourceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedIngestionService.class);

    private final RssSourceRepository rssSourceRepository;
    private final NewsItemRepository newsItemRepository;
    private final SourceDeduplicationService sourceDeduplicationService;
    private final CategorizationService categorizationService;
    private final RssClient rssClient;
    private final RssFeedParser rssFeedParser;
    private final Clock clock;

    @Autowired
    public FeedIngestionService(
            RssSourceRepository rssSourceRepository,
            NewsItemRepository newsItemRepository,
            SourceDeduplicationService sourceDeduplicationService,
            CategorizationService categorizationService,
            RssClient rssClient,
            RssFeedParser rssFeedParser) {
        this(
                rssSourceRepository,
                newsItemRepository,
                sourceDeduplicationService,
                categorizationService,
                rssClient,
                rssFeedParser,
                Clock.systemUTC());
    }

    FeedIngestionService(
            RssSourceRepository rssSourceRepository,
            NewsItemRepository newsItemRepository,
            SourceDeduplicationService sourceDeduplicationService,
            CategorizationService categorizationService,
            RssClient rssClient,
            RssFeedParser rssFeedParser,
            Clock clock) {
        this.rssSourceRepository = rssSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.sourceDeduplicationService = sourceDeduplicationService;
        this.categorizationService = categorizationService;
        this.rssClient = rssClient;
        this.rssFeedParser = rssFeedParser;
        this.clock = clock;
    }

    @Transactional
    public void ingestEnabledSources() {
        List<RssSource> sources = rssSourceRepository.findByEnabledTrue();
        LOGGER.info("Starting RSS ingestion for {} enabled sources", sources.size());
        sources.forEach(this::ingestSource);
    }

    private void ingestSource(RssSource source) {
        try {
            String xml = rssClient.fetch(URI.create(source.getUrl()));
            List<RssFeedItemDto> feedItems = rssFeedParser.parse(xml);
            Instant fetchedAt = Instant.now(clock);
            List<NewsItem> newsItems = sourceDeduplicationService.newItemsForSource(source, feedItems, fetchedAt);

            if (newsItems.isEmpty()) {
                LOGGER.info("Skipped {} duplicate RSS items from source {}", feedItems.size(), source.getName());
                return;
            }

            List<NewsItem> savedNewsItems = newsItemRepository.saveAll(newsItems);
            categorizationService.classifyNewItems(savedNewsItems);
            LOGGER.info(
                    "Ingested {} new RSS items from source {}; skipped {} duplicates",
                    newsItems.size(),
                    source.getName(),
                    feedItems.size() - newsItems.size());
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to ingest RSS source {} ({})", source.getName(), source.getUrl(), exception);
        }
    }
}

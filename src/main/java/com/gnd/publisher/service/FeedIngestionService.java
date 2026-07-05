package com.gnd.publisher.service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    private final SourceQuotaSelectionService sourceQuotaSelectionService;
    private final RssClient rssClient;
    private final RssFeedParser rssFeedParser;
    private final Clock clock;

    @Autowired
    public FeedIngestionService(
            RssSourceRepository rssSourceRepository,
            NewsItemRepository newsItemRepository,
            SourceDeduplicationService sourceDeduplicationService,
            CategorizationService categorizationService,
            SourceQuotaSelectionService sourceQuotaSelectionService,
            RssClient rssClient,
            RssFeedParser rssFeedParser) {
        this(
                rssSourceRepository,
                newsItemRepository,
                sourceDeduplicationService,
                categorizationService,
                sourceQuotaSelectionService,
                rssClient,
                rssFeedParser,
                Clock.systemUTC());
    }

    FeedIngestionService(
            RssSourceRepository rssSourceRepository,
            NewsItemRepository newsItemRepository,
            SourceDeduplicationService sourceDeduplicationService,
            CategorizationService categorizationService,
            SourceQuotaSelectionService sourceQuotaSelectionService,
            RssClient rssClient,
            RssFeedParser rssFeedParser,
            Clock clock) {
        this.rssSourceRepository = rssSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.sourceDeduplicationService = sourceDeduplicationService;
        this.categorizationService = categorizationService;
        this.sourceQuotaSelectionService = sourceQuotaSelectionService;
        this.rssClient = rssClient;
        this.rssFeedParser = rssFeedParser;
        this.clock = clock;
    }

    @Transactional
    public void ingestEnabledSources() {
        List<RssSource> sources = rssSourceRepository.findByEnabledTrue();
        if (sources.isEmpty()) {
            LOGGER.info("No enabled RSS sources found for ingestion");
            return;
        }

        String processingRunId = processingRunId();
        LOGGER.info("Starting RSS ingestion run {} for {} enabled sources", processingRunId, sources.size());
        sources.forEach(source -> ingestSource(source, processingRunId));
        sourceQuotaSelectionService.selectForProcessingRun(processingRunId);
    }

    private void ingestSource(RssSource source, String processingRunId) {
        try {
            String xml = rssClient.fetch(URI.create(source.getUrl()));
            List<RssFeedItemDto> feedItems = rssFeedParser.parse(xml);
            Instant fetchedAt = Instant.now(clock);
            List<NewsItem> newsItems = sourceDeduplicationService.newItemsForSource(source, feedItems, fetchedAt);

            if (newsItems.isEmpty()) {
                LOGGER.info("Skipped {} duplicate RSS items from source {}", feedItems.size(), source.getName());
                return;
            }

            newsItems.forEach(newsItem -> newsItem.assignProcessingRun(processingRunId));
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

    private String processingRunId() {
        return "ingestion-" + Instant.now(clock) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

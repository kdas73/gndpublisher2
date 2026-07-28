package com.gnd.publisher.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.gnd.publisher.config.CleanupProperties;
import com.gnd.publisher.domain.model.ImportantNewsDigestPost;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.Publication;
import com.gnd.publisher.repository.ClassificationRunRepository;
import com.gnd.publisher.repository.ImportantNewsDigestItemRepository;
import com.gnd.publisher.repository.ImportantNewsDigestPostRepository;
import com.gnd.publisher.repository.NewsItemCategoryRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.NewsSummaryRepository;
import com.gnd.publisher.repository.PublicationRepository;
import com.gnd.publisher.repository.TranslationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

    private final ClassificationRunRepository classificationRunRepository;
    private final ImportantNewsDigestPostRepository importantNewsDigestPostRepository;
    private final ImportantNewsDigestItemRepository importantNewsDigestItemRepository;
    private final PublicationRepository publicationRepository;
    private final NewsItemRepository newsItemRepository;
    private final TranslationRepository translationRepository;
    private final NewsItemCategoryRepository newsItemCategoryRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final CleanupProperties cleanupProperties;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    @Autowired
    public CleanupService(
            ClassificationRunRepository classificationRunRepository,
            ImportantNewsDigestPostRepository importantNewsDigestPostRepository,
            ImportantNewsDigestItemRepository importantNewsDigestItemRepository,
            PublicationRepository publicationRepository,
            NewsItemRepository newsItemRepository,
            TranslationRepository translationRepository,
            NewsItemCategoryRepository newsItemCategoryRepository,
            NewsSummaryRepository newsSummaryRepository,
            CleanupProperties cleanupProperties,
            PlatformTransactionManager transactionManager) {
        this(
                classificationRunRepository,
                importantNewsDigestPostRepository,
                importantNewsDigestItemRepository,
                publicationRepository,
                newsItemRepository,
                translationRepository,
                newsItemCategoryRepository,
                newsSummaryRepository,
                cleanupProperties,
                new TransactionTemplate(transactionManager),
                Clock.systemUTC());
    }

    CleanupService(
            ClassificationRunRepository classificationRunRepository,
            ImportantNewsDigestPostRepository importantNewsDigestPostRepository,
            ImportantNewsDigestItemRepository importantNewsDigestItemRepository,
            PublicationRepository publicationRepository,
            NewsItemRepository newsItemRepository,
            TranslationRepository translationRepository,
            NewsItemCategoryRepository newsItemCategoryRepository,
            NewsSummaryRepository newsSummaryRepository,
            CleanupProperties cleanupProperties,
            TransactionOperations transactionOperations,
            Clock clock) {
        this.classificationRunRepository = classificationRunRepository;
        this.importantNewsDigestPostRepository = importantNewsDigestPostRepository;
        this.importantNewsDigestItemRepository = importantNewsDigestItemRepository;
        this.publicationRepository = publicationRepository;
        this.newsItemRepository = newsItemRepository;
        this.translationRepository = translationRepository;
        this.newsItemCategoryRepository = newsItemCategoryRepository;
        this.newsSummaryRepository = newsSummaryRepository;
        this.cleanupProperties = cleanupProperties;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
    }

    public void runCleanup() {
        runStep("classification runs", this::cleanupClassificationRuns);
        runStep("important news digest", this::cleanupImportantNewsDigest);
        runStep("publications", this::cleanupPublications);
        runStep("news items", this::cleanupNewsItems);
    }

    private void runStep(String stepName, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException exception) {
            LOGGER.warn("Cleanup step '{}' failed", stepName, exception);
        }
    }

    private void cleanupClassificationRuns() {
        Instant cutoff = cutoff(cleanupProperties.classificationRunsRetention());
        transactionOperations.executeWithoutResult(status ->
                classificationRunRepository.deleteByCreatedAtBefore(cutoff));
    }

    private void cleanupImportantNewsDigest() {
        Instant cutoff = cutoff(cleanupProperties.digestRetention());
        transactionOperations.executeWithoutResult(status -> {
            List<ImportantNewsDigestPost> posts = importantNewsDigestPostRepository.findByCreatedAtBefore(cutoff);
            posts.forEach(post ->
                    importantNewsDigestItemRepository.deleteByImportantNewsDigestPost_Id(post.getId()));
            importantNewsDigestPostRepository.deleteAll(posts);
        });
    }

    private void cleanupPublications() {
        Instant cutoff = cutoff(cleanupProperties.publicationsRetention());
        transactionOperations.executeWithoutResult(status -> {
            List<Publication> candidates = publicationRepository.findCleanupCandidates(cutoff);
            publicationRepository.deleteAll(candidates);
        });
    }

    private void cleanupNewsItems() {
        Instant cutoff = cutoff(cleanupProperties.newsItemsRetention());
        transactionOperations.executeWithoutResult(status -> {
            List<NewsItem> candidates = newsItemRepository.findCleanupCandidates(cutoff);
            candidates.forEach(item -> {
                translationRepository.deleteByNewsItem_Id(item.getId());
                newsItemCategoryRepository.deleteByNewsItem_Id(item.getId());
                classificationRunRepository.deleteByNewsItem_Id(item.getId());
                newsSummaryRepository.deleteByNewsItem_Id(item.getId());
            });
            newsItemRepository.deleteAll(candidates);
        });
    }

    private Instant cutoff(Duration retention) {
        return Instant.now(clock).minus(retention);
    }
}

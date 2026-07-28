package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.gnd.publisher.config.CleanupProperties;
import com.gnd.publisher.domain.model.ImportantNewsDigestPost;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.Publication;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.repository.CategoryRepository;
import com.gnd.publisher.repository.ClassificationRunRepository;
import com.gnd.publisher.repository.ImportantNewsDigestItemRepository;
import com.gnd.publisher.repository.ImportantNewsDigestPostRepository;
import com.gnd.publisher.repository.NewsItemCategoryRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.NewsSummaryRepository;
import com.gnd.publisher.repository.PublicationRepository;
import com.gnd.publisher.repository.RssSourceRepository;
import com.gnd.publisher.repository.TelegramChannelRepository;
import com.gnd.publisher.repository.TranslationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class CleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final Duration NEWS_ITEMS_RETENTION = Duration.ofDays(30);
    private static final Duration CLASSIFICATION_RUNS_RETENTION = Duration.ofDays(5);
    private static final Duration PUBLICATIONS_RETENTION = Duration.ofDays(30);
    private static final Duration DIGEST_RETENTION = Duration.ofDays(30);

    @Mock
    private ClassificationRunRepository classificationRunRepository;

    @Mock
    private ImportantNewsDigestPostRepository importantNewsDigestPostRepository;

    @Mock
    private ImportantNewsDigestItemRepository importantNewsDigestItemRepository;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private NewsItemCategoryRepository newsItemCategoryRepository;

    @Mock
    private NewsSummaryRepository newsSummaryRepository;

    @Test
    void computesCutoffsForEachRetentionPeriod() {
        when(newsItemRepository.findCleanupCandidates(any())).thenReturn(List.of());
        when(importantNewsDigestPostRepository.findByCreatedAtBefore(any())).thenReturn(List.of());
        when(publicationRepository.findCleanupCandidates(any())).thenReturn(List.of());

        service().runCleanup();

        verify(classificationRunRepository).deleteByCreatedAtBefore(NOW.minus(CLASSIFICATION_RUNS_RETENTION));
        verify(importantNewsDigestPostRepository).findByCreatedAtBefore(NOW.minus(DIGEST_RETENTION));
        verify(publicationRepository).findCleanupCandidates(NOW.minus(PUBLICATIONS_RETENTION));
        verify(newsItemRepository).findCleanupCandidates(NOW.minus(NEWS_ITEMS_RETENTION));
    }

    @Test
    void deletesNewsItemChildrenBeforeTheNewsItemsThemselves() {
        NewsItem newsItem = newsItem();
        ReflectionTestUtils.setField(newsItem, "id", 500L);
        when(newsItemRepository.findCleanupCandidates(any())).thenReturn(List.of(newsItem));
        when(importantNewsDigestPostRepository.findByCreatedAtBefore(any())).thenReturn(List.of());
        when(publicationRepository.findCleanupCandidates(any())).thenReturn(List.of());

        service().runCleanup();

        verify(translationRepository).deleteByNewsItem_Id(500L);
        verify(newsItemCategoryRepository).deleteByNewsItem_Id(500L);
        verify(classificationRunRepository).deleteByNewsItem_Id(500L);
        verify(newsSummaryRepository).deleteByNewsItem_Id(500L);
        verify(newsItemRepository).deleteAll(List.of(newsItem));
    }

    @Test
    void deletesDigestItemsBeforeDigestPosts() {
        TelegramChannel channel = TelegramChannel.create(
                "important-news-ru", "ru", "-100900", "gnd_important_news",
                "https://t.me/{username}/{messageId}", "Important News RU", true);
        ImportantNewsDigestPost post = ImportantNewsDigestPost.pending(channel, "ru", 2);
        ReflectionTestUtils.setField(post, "id", 700L);
        when(importantNewsDigestPostRepository.findByCreatedAtBefore(any())).thenReturn(List.of(post));
        when(publicationRepository.findCleanupCandidates(any())).thenReturn(List.of());
        when(newsItemRepository.findCleanupCandidates(any())).thenReturn(List.of());

        service().runCleanup();

        verify(importantNewsDigestItemRepository).deleteByImportantNewsDigestPost_Id(700L);
        verify(importantNewsDigestPostRepository).deleteAll(List.of(post));
    }

    @Test
    void deletesPublicationCleanupCandidates() {
        SemanticNewsEvent semanticEvent = semanticNewsEvent();
        NewsItem newsItem = newsItem();
        Translation translation = Translation.publicationContent(
                semanticEvent, newsItem, "ru", "Title", "Summary", "openai", "gpt-5.5");
        TelegramChannel channel = TelegramChannel.create(
                "news-ru", "ru", "-100111", "gnd_news",
                "https://t.me/{username}/{messageId}", "News RU", true);
        Publication publication = Publication.pending(semanticEvent, newsItem, translation, channel, "ru");
        when(publicationRepository.findCleanupCandidates(any())).thenReturn(List.of(publication));
        when(importantNewsDigestPostRepository.findByCreatedAtBefore(any())).thenReturn(List.of());
        when(newsItemRepository.findCleanupCandidates(any())).thenReturn(List.of());

        service().runCleanup();

        verify(publicationRepository).deleteAll(List.of(publication));
    }

    @Test
    void continuesRemainingStepsWhenOneStepFails() {
        org.mockito.Mockito.doThrow(new RuntimeException("db unavailable"))
                .when(classificationRunRepository).deleteByCreatedAtBefore(any());
        when(importantNewsDigestPostRepository.findByCreatedAtBefore(any())).thenReturn(List.of());
        when(publicationRepository.findCleanupCandidates(any())).thenReturn(List.of());
        when(newsItemRepository.findCleanupCandidates(any())).thenReturn(List.of());

        service().runCleanup();

        verify(importantNewsDigestPostRepository, times(1)).findByCreatedAtBefore(any());
        verify(publicationRepository, times(1)).findCleanupCandidates(any());
        verify(newsItemRepository, times(1)).findCleanupCandidates(any());
    }

    @Test
    void doesNotDependOnConfigurationRepositories() {
        Constructor<?>[] constructors = CleanupService.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            List<Class<?>> parameterTypes = List.of(constructor.getParameterTypes());
            assertThat(parameterTypes).doesNotContain(
                    CategoryRepository.class, RssSourceRepository.class, TelegramChannelRepository.class);
        }
    }

    private CleanupService service() {
        return new CleanupService(
                classificationRunRepository,
                importantNewsDigestPostRepository,
                importantNewsDigestItemRepository,
                publicationRepository,
                newsItemRepository,
                translationRepository,
                newsItemCategoryRepository,
                newsSummaryRepository,
                cleanupProperties(),
                transactionOperations(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private CleanupProperties cleanupProperties() {
        return new CleanupProperties(
                NEWS_ITEMS_RETENTION,
                CLASSIFICATION_RUNS_RETENTION,
                PUBLICATIONS_RETENTION,
                DIGEST_RETENTION);
    }

    private TransactionOperations transactionOperations() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }

    private NewsItem newsItem() {
        RssSource source = RssSource.create(
                "ert-news", "ERT News", "https://feeds.example.test/ert", "el", true);
        NewsItem newsItem = NewsItem.fromRssFeedItem(
                source,
                new RssFeedItemDto(
                        Optional.of("external-1"),
                        "Source title",
                        Optional.of("https://example.test/news/item"),
                        Optional.of("Source summary"),
                        Optional.of(NOW)),
                NOW);
        newsItem.markClassificationFailed(NOW);
        return newsItem;
    }

    private SemanticNewsEvent semanticNewsEvent() {
        SemanticNewsEvent semanticEvent = SemanticNewsEvent.create("source event", null, NOW);
        ReflectionTestUtils.setField(semanticEvent, "id", 900L);
        return semanticEvent;
    }
}

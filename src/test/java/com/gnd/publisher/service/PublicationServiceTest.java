package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.Publication;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;
import com.gnd.publisher.dto.telegram.TelegramSendResult;
import com.gnd.publisher.exception.TelegramPublishException;
import com.gnd.publisher.integration.telegram.TelegramBotClient;
import com.gnd.publisher.mapper.TelegramMessageMapper;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.PublicationRepository;
import com.gnd.publisher.repository.TranslationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private PublicationContentService publicationContentService;

    @Mock
    private TelegramChannelSyncService telegramChannelSyncService;

    @Mock
    private TelegramRoutingService telegramRoutingService;

    @Mock
    private TelegramMessageMapper telegramMessageMapper;

    @Mock
    private TelegramBotClient telegramBotClient;

    @Test
    void publishesSelectedTranslationToRoutedChannel() {
        PublicationContext context = publicationContext();
        when(newsItemRepository.findBySelectedForPublicationTrueAndPublicationProcessedAtIsNullAndSemanticEventIsNotNull())
                .thenReturn(List.of(context.newsItem()));
        when(publicationContentService.preparePublicationContent(List.of(context.newsItem())))
                .thenReturn(List.of(context.translation()));
        when(translationRepository.findWithNewsItemById(300L)).thenReturn(Optional.of(context.translation()));
        when(telegramRoutingService.publicationChannelsForLanguage("en")).thenReturn(List.of(context.channel()));
        when(publicationRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(200L, 400L, "en"))
                .thenReturn(false);
        when(publicationRepository.saveAndFlush(any(Publication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(telegramMessageMapper.toMessage(context.translation(), context.channel()))
                .thenReturn(new TelegramMessageDto("-100123456", "message"));
        when(telegramBotClient.sendMessage(context.channel(), new TelegramMessageDto("-100123456", "message")))
                .thenReturn(new TelegramSendResult("42", "https://t.me/gnd_news/42"));

        List<Publication> publications = service().publishSelectedContent();

        assertThat(publications).singleElement().satisfies(publication -> {
            assertThat(publication.getStatus().name()).isEqualTo("PUBLISHED");
            assertThat(publication.getTelegramMessageId()).isEqualTo("42");
            assertThat(publication.getTelegramMessageUrl()).isEqualTo("https://t.me/gnd_news/42");
            assertThat(publication.getPublishedAt()).isEqualTo(NOW);
        });
        verify(telegramChannelSyncService).syncConfiguredChannels();
        assertThat(context.newsItem().isSelectedForPublication()).isFalse();
        assertThat(context.newsItem().getPublicationProcessedAt()).isEqualTo(NOW);
        verify(newsItemRepository).saveAll(List.of(context.newsItem()));
    }

    @Test
    void skipsDuplicateSemanticEventLanguageChannelPublication() {
        PublicationContext context = publicationContext();
        when(newsItemRepository.findBySelectedForPublicationTrueAndPublicationProcessedAtIsNullAndSemanticEventIsNotNull())
                .thenReturn(List.of(context.newsItem()));
        when(publicationContentService.preparePublicationContent(List.of(context.newsItem())))
                .thenReturn(List.of(context.translation()));
        when(translationRepository.findWithNewsItemById(300L)).thenReturn(Optional.of(context.translation()));
        when(telegramRoutingService.publicationChannelsForLanguage("en")).thenReturn(List.of(context.channel()));
        when(publicationRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(200L, 400L, "en"))
                .thenReturn(true);

        List<Publication> publications = service().publishSelectedContent();

        assertThat(publications).isEmpty();
        verify(telegramBotClient, never()).sendMessage(any(), any());
        verify(publicationRepository, never()).saveAndFlush(any());
        assertThat(context.newsItem().isSelectedForPublication()).isFalse();
        assertThat(context.newsItem().getPublicationProcessedAt()).isEqualTo(NOW);
        verify(newsItemRepository).saveAll(List.of(context.newsItem()));
    }

    @Test
    void persistsFailureWhenTelegramSendFails() {
        PublicationContext context = publicationContext();
        when(newsItemRepository.findBySelectedForPublicationTrueAndPublicationProcessedAtIsNullAndSemanticEventIsNotNull())
                .thenReturn(List.of(context.newsItem()));
        when(publicationContentService.preparePublicationContent(List.of(context.newsItem())))
                .thenReturn(List.of(context.translation()));
        when(translationRepository.findWithNewsItemById(300L)).thenReturn(Optional.of(context.translation()));
        when(telegramRoutingService.publicationChannelsForLanguage("en")).thenReturn(List.of(context.channel()));
        when(publicationRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(200L, 400L, "en"))
                .thenReturn(false);
        when(publicationRepository.saveAndFlush(any(Publication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(telegramMessageMapper.toMessage(context.translation(), context.channel()))
                .thenReturn(new TelegramMessageDto("-100123456", "message"));
        when(telegramBotClient.sendMessage(context.channel(), new TelegramMessageDto("-100123456", "message")))
                .thenThrow(new TelegramPublishException("Telegram returned HTTP status 400"));

        List<Publication> publications = service().publishSelectedContent();

        assertThat(publications).singleElement().satisfies(publication -> {
            assertThat(publication.getStatus().name()).isEqualTo("FAILED");
            assertThat(publication.getErrorMessage()).contains("Telegram returned HTTP status 400");
        });
        assertThat(context.newsItem().isSelectedForPublication()).isFalse();
        assertThat(context.newsItem().getPublicationProcessedAt()).isEqualTo(NOW);
        verify(newsItemRepository).saveAll(List.of(context.newsItem()));
    }

    private PublicationService service() {
        return new PublicationService(
                newsItemRepository,
                translationRepository,
                publicationRepository,
                publicationContentService,
                telegramChannelSyncService,
                telegramRoutingService,
                telegramMessageMapper,
                telegramBotClient,
                transactionOperations(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private TransactionOperations transactionOperations() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }

    private PublicationContext publicationContext() {
        RssSource source = RssSource.create(
                "ert-news",
                "ERT News",
                "https://feeds.example.test/ert",
                "el",
                true);
        ReflectionTestUtils.setField(source, "id", 100L);
        NewsItem newsItem = NewsItem.fromRssFeedItem(
                source,
                new RssFeedItemDto(
                        Optional.of("external-1"),
                        "Source title",
                        Optional.of("https://example.test/news/item"),
                        Optional.of("Source summary"),
                        Optional.of(NOW)),
                NOW);
        ReflectionTestUtils.setField(newsItem, "id", 100L);
        Category category = Category.create("politics", "Politics", "Government", true, true, 10);
        SemanticNewsEvent semanticEvent = SemanticNewsEvent.create("source event", category, NOW);
        ReflectionTestUtils.setField(semanticEvent, "id", 200L);
        newsItem.markClassified(semanticEvent, java.math.BigDecimal.valueOf(0.9), true, null, NOW);
        newsItem.markSelectedForPublication();
        Translation translation = Translation.publicationContent(
                semanticEvent,
                newsItem,
                "en",
                "Translated title",
                "Translated summary",
                "openai",
                "gpt-5.5");
        ReflectionTestUtils.setField(translation, "id", 300L);
        TelegramChannel channel = TelegramChannel.create(
                "news-en",
                "en",
                "-100123456",
                "gnd_news",
                "https://t.me/{username}/{messageId}",
                "News EN",
                true);
        ReflectionTestUtils.setField(channel, "id", 400L);
        return new PublicationContext(newsItem, translation, channel);
    }

    private record PublicationContext(
            NewsItem newsItem,
            Translation translation,
            TelegramChannel channel) {
    }
}

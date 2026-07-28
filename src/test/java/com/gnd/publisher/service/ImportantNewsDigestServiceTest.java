package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.gnd.publisher.config.ImportantNewsDigestProperties;
import com.gnd.publisher.domain.enums.PublicationStatus;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.ImportantNewsDigestPost;
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
import com.gnd.publisher.mapper.ImportantNewsDigestMessageMapper;
import com.gnd.publisher.repository.ImportantNewsDigestItemRepository;
import com.gnd.publisher.repository.ImportantNewsDigestPostRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.PublicationRepository;
import com.gnd.publisher.repository.SemanticEventSourceItemCount;
import com.gnd.publisher.repository.TelegramChannelRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class ImportantNewsDigestServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant SINCE = NOW.minus(Duration.ofDays(7));
    private static final String TARGET_LANGUAGE = "ru";
    private static final String CHANNEL_CODE = "important-news-ru";

    @Mock
    private TelegramChannelRepository telegramChannelRepository;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private ImportantNewsDigestItemRepository importantNewsDigestItemRepository;

    @Mock
    private ImportantNewsDigestPostRepository importantNewsDigestPostRepository;

    @Mock
    private ImportantNewsDigestMessageMapper importantNewsDigestMessageMapper;

    @Mock
    private TelegramBotClient telegramBotClient;

    @Test
    void publishesDigestWhenCandidateExceedsThreshold() {
        DigestFixture fixture = digestFixture(600L, 3);
        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.of(fixture.digestChannel()));
        when(publicationRepository.findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                PublicationStatus.PUBLISHED, TARGET_LANGUAGE, SINCE))
                .thenReturn(List.of(fixture.publication()));
        when(newsItemRepository.countBySemanticEventIds(List.of(600L)))
                .thenReturn(List.of(new SemanticEventSourceItemCount(600L, 3)));
        when(importantNewsDigestItemRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
                600L, 900L, TARGET_LANGUAGE)).thenReturn(false);
        when(importantNewsDigestPostRepository.save(any(ImportantNewsDigestPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importantNewsDigestMessageMapper.toMessage(anyList(), any(TelegramChannel.class)))
                .thenReturn(new TelegramMessageDto("-100999", "digest message"));
        when(telegramBotClient.sendMessage(any(TelegramChannel.class), any(TelegramMessageDto.class)))
                .thenReturn(new TelegramSendResult("55", "https://t.me/gnd_important_news/55"));

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(result.get().getTelegramMessageId()).isEqualTo("55");
        assertThat(result.get().getTelegramMessageUrl()).isEqualTo("https://t.me/gnd_important_news/55");
        assertThat(result.get().getPublishedAt()).isEqualTo(NOW);
        verify(importantNewsDigestItemRepository).saveAll(anyList());
    }

    @Test
    void excludesCandidateAtOrBelowThreshold() {
        DigestFixture fixture = digestFixture(601L, 2);
        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.of(fixture.digestChannel()));
        when(publicationRepository.findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                PublicationStatus.PUBLISHED, TARGET_LANGUAGE, SINCE))
                .thenReturn(List.of(fixture.publication()));
        when(newsItemRepository.countBySemanticEventIds(List.of(601L)))
                .thenReturn(List.of(new SemanticEventSourceItemCount(601L, 2)));

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isEmpty();
        verify(telegramBotClient, never()).sendMessage(any(), any());
        verify(importantNewsDigestPostRepository, never()).save(any());
    }

    @Test
    void skipsAlreadyDigestedSemanticEvent() {
        DigestFixture fixture = digestFixture(602L, 5);
        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.of(fixture.digestChannel()));
        when(publicationRepository.findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                PublicationStatus.PUBLISHED, TARGET_LANGUAGE, SINCE))
                .thenReturn(List.of(fixture.publication()));
        when(newsItemRepository.countBySemanticEventIds(List.of(602L)))
                .thenReturn(List.of(new SemanticEventSourceItemCount(602L, 5)));
        when(importantNewsDigestItemRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
                602L, 900L, TARGET_LANGUAGE)).thenReturn(true);

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isEmpty();
        verify(telegramBotClient, never()).sendMessage(any(), any());
    }

    @Test
    void returnsEmptyWhenNoCandidatesPublished() {
        when(telegramChannelRepository.findByCode(CHANNEL_CODE))
                .thenReturn(Optional.of(digestChannel()));
        when(publicationRepository.findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                PublicationStatus.PUBLISHED, TARGET_LANGUAGE, SINCE))
                .thenReturn(List.of());

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isEmpty();
        verify(newsItemRepository, never()).countBySemanticEventIds(anyList());
    }

    @Test
    void returnsEmptyWhenDigestChannelMissing() {
        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.empty());

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isEmpty();
        verify(publicationRepository, never())
                .findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(any(), any(), any());
    }

    @Test
    void returnsEmptyWhenDigestChannelDisabled() {
        TelegramChannel disabledChannel = TelegramChannel.create(
                CHANNEL_CODE, TARGET_LANGUAGE, "-100900", "gnd_important_news",
                "https://t.me/{username}/{messageId}", "Important News RU", false);
        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.of(disabledChannel));

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isEmpty();
        verify(publicationRepository, never())
                .findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(any(), any(), any());
    }

    @Test
    void persistsFailedPostWhenTelegramSendThrows() {
        DigestFixture fixture = digestFixture(603L, 4);
        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.of(fixture.digestChannel()));
        when(publicationRepository.findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                PublicationStatus.PUBLISHED, TARGET_LANGUAGE, SINCE))
                .thenReturn(List.of(fixture.publication()));
        when(newsItemRepository.countBySemanticEventIds(List.of(603L)))
                .thenReturn(List.of(new SemanticEventSourceItemCount(603L, 4)));
        when(importantNewsDigestItemRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
                603L, 900L, TARGET_LANGUAGE)).thenReturn(false);
        when(importantNewsDigestPostRepository.save(any(ImportantNewsDigestPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importantNewsDigestMessageMapper.toMessage(anyList(), any(TelegramChannel.class)))
                .thenReturn(new TelegramMessageDto("-100999", "digest message"));
        when(telegramBotClient.sendMessage(any(TelegramChannel.class), any(TelegramMessageDto.class)))
                .thenThrow(new TelegramPublishException("Telegram returned HTTP status 400"));

        Optional<ImportantNewsDigestPost> result = service().publishImportantNewsDigest();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PublicationStatus.FAILED);
        assertThat(result.get().getErrorMessage()).contains("Telegram returned HTTP status 400");
        verify(importantNewsDigestItemRepository).saveAll(anyList());
    }

    @Test
    void deduplicatesMultiplePublicationsForSameSemanticEvent() {
        TelegramChannel digestChannel = digestChannel();
        SemanticNewsEvent semanticEvent = semanticNewsEvent(604L);
        NewsItem newsItem = newsItem();
        Translation translation = translation(semanticEvent, newsItem, "Duplicated title");
        TelegramChannel channelOne = regularChannel("news-ru", "-100111");
        TelegramChannel channelTwo = regularChannel("news-ru-2", "-100222");
        Publication publicationOne = publication(semanticEvent, newsItem, translation, channelOne, "10", "https://t.me/news_ru/10");
        Publication publicationTwo = publication(semanticEvent, newsItem, translation, channelTwo, "20", "https://t.me/news_ru_2/20");

        when(telegramChannelRepository.findByCode(CHANNEL_CODE)).thenReturn(Optional.of(digestChannel));
        when(publicationRepository.findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                PublicationStatus.PUBLISHED, TARGET_LANGUAGE, SINCE))
                .thenReturn(List.of(publicationOne, publicationTwo));
        when(newsItemRepository.countBySemanticEventIds(List.of(604L)))
                .thenReturn(List.of(new SemanticEventSourceItemCount(604L, 5)));
        when(importantNewsDigestItemRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
                604L, 900L, TARGET_LANGUAGE)).thenReturn(false);
        when(importantNewsDigestPostRepository.save(any(ImportantNewsDigestPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importantNewsDigestMessageMapper.toMessage(anyList(), any(TelegramChannel.class)))
                .thenReturn(new TelegramMessageDto("-100999", "digest message"));
        when(telegramBotClient.sendMessage(any(TelegramChannel.class), any(TelegramMessageDto.class)))
                .thenReturn(new TelegramSendResult("55", "https://t.me/gnd_important_news/55"));

        service().publishImportantNewsDigest();

        var itemsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(importantNewsDigestItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
    }

    private ImportantNewsDigestService service() {
        return new ImportantNewsDigestService(
                telegramChannelRepository,
                publicationRepository,
                newsItemRepository,
                importantNewsDigestItemRepository,
                importantNewsDigestPostRepository,
                importantNewsDigestMessageMapper,
                telegramBotClient,
                digestProperties(),
                transactionOperations(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ImportantNewsDigestProperties digestProperties() {
        return new ImportantNewsDigestProperties(
                true,
                "0 0 */6 * * *",
                2,
                TARGET_LANGUAGE,
                CHANNEL_CODE,
                Duration.ofDays(7));
    }

    private TransactionOperations transactionOperations() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }

    private DigestFixture digestFixture(long semanticEventId, int sourceItemCount) {
        TelegramChannel channel = digestChannel();
        SemanticNewsEvent semanticEvent = semanticNewsEvent(semanticEventId);
        NewsItem item = newsItem();
        Translation translation = translation(semanticEvent, item, "Important title " + semanticEventId);
        TelegramChannel regularChannel = regularChannel("news-ru", "-100111");
        Publication publication = publication(
                semanticEvent, item, translation, regularChannel, "10", "https://t.me/news_ru/10");
        return new DigestFixture(channel, publication);
    }

    private SemanticNewsEvent semanticNewsEvent(long id) {
        Category category = Category.create("politics", "Politics", "Government", true, true, 10);
        SemanticNewsEvent semanticEvent = SemanticNewsEvent.create("source event " + id, category, NOW);
        ReflectionTestUtils.setField(semanticEvent, "id", id);
        return semanticEvent;
    }

    private NewsItem newsItem() {
        RssSource source = RssSource.create(
                "ert-news", "ERT News", "https://feeds.example.test/ert", "el", true);
        return NewsItem.fromRssFeedItem(
                source,
                new RssFeedItemDto(
                        Optional.of("external-1"),
                        "Source title",
                        Optional.of("https://example.test/news/item"),
                        Optional.of("Source summary"),
                        Optional.of(NOW)),
                NOW);
    }

    private Translation translation(SemanticNewsEvent semanticEvent, NewsItem newsItem, String title) {
        return Translation.publicationContent(
                semanticEvent, newsItem, TARGET_LANGUAGE, title, "Translated summary", "openai", "gpt-5.5");
    }

    private Publication publication(
            SemanticNewsEvent semanticEvent,
            NewsItem newsItem,
            Translation translation,
            TelegramChannel channel,
            String messageId,
            String messageUrl) {
        Publication publication = Publication.pending(semanticEvent, newsItem, translation, channel, TARGET_LANGUAGE);
        publication.markPublished(messageId, messageUrl, NOW);
        return publication;
    }

    private TelegramChannel digestChannel() {
        TelegramChannel channel = TelegramChannel.create(
                CHANNEL_CODE, TARGET_LANGUAGE, "-100900", "gnd_important_news",
                "https://t.me/{username}/{messageId}", "Important News RU", true);
        ReflectionTestUtils.setField(channel, "id", 900L);
        return channel;
    }

    private TelegramChannel regularChannel(String code, String channelId) {
        return TelegramChannel.create(
                code, TARGET_LANGUAGE, channelId, "gnd_news",
                "https://t.me/{username}/{messageId}", "News RU", true);
    }

    private record DigestFixture(TelegramChannel digestChannel, Publication publication) {
    }
}

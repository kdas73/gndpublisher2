package com.gnd.publisher.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.Publication;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;
import com.gnd.publisher.dto.telegram.TelegramSendResult;
import com.gnd.publisher.integration.telegram.TelegramBotClient;
import com.gnd.publisher.logging.LogFields;
import com.gnd.publisher.logging.LoggingContext;
import com.gnd.publisher.mapper.TelegramMessageMapper;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.PublicationRepository;
import com.gnd.publisher.repository.TranslationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PublicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationService.class);

    private final NewsItemRepository newsItemRepository;
    private final TranslationRepository translationRepository;
    private final PublicationRepository publicationRepository;
    private final PublicationContentService publicationContentService;
    private final TelegramChannelSyncService telegramChannelSyncService;
    private final TelegramRoutingService telegramRoutingService;
    private final TelegramMessageMapper telegramMessageMapper;
    private final TelegramBotClient telegramBotClient;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    @Autowired
    public PublicationService(
            NewsItemRepository newsItemRepository,
            TranslationRepository translationRepository,
            PublicationRepository publicationRepository,
            PublicationContentService publicationContentService,
            TelegramChannelSyncService telegramChannelSyncService,
            TelegramRoutingService telegramRoutingService,
            TelegramMessageMapper telegramMessageMapper,
            TelegramBotClient telegramBotClient,
            PlatformTransactionManager transactionManager) {
        this(
                newsItemRepository,
                translationRepository,
                publicationRepository,
                publicationContentService,
                telegramChannelSyncService,
                telegramRoutingService,
                telegramMessageMapper,
                telegramBotClient,
                new TransactionTemplate(transactionManager),
                Clock.systemUTC());
    }

    PublicationService(
            NewsItemRepository newsItemRepository,
            TranslationRepository translationRepository,
            PublicationRepository publicationRepository,
            PublicationContentService publicationContentService,
            TelegramChannelSyncService telegramChannelSyncService,
            TelegramRoutingService telegramRoutingService,
            TelegramMessageMapper telegramMessageMapper,
            TelegramBotClient telegramBotClient,
            TransactionOperations transactionOperations,
            Clock clock) {
        this.newsItemRepository = newsItemRepository;
        this.translationRepository = translationRepository;
        this.publicationRepository = publicationRepository;
        this.publicationContentService = publicationContentService;
        this.telegramChannelSyncService = telegramChannelSyncService;
        this.telegramRoutingService = telegramRoutingService;
        this.telegramMessageMapper = telegramMessageMapper;
        this.telegramBotClient = telegramBotClient;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
    }

    public List<Publication> publishSelectedContent() {
        try {
            telegramChannelSyncService.syncConfiguredChannels();
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Failed to sync configured Telegram channels; continuing with existing channel configuration",
                    exception);
        }
        List<NewsItem> selectedItems = newsItemRepository
                .findBySelectedForPublicationTrueAndPublicationProcessedAtIsNullAndSemanticEventIsNotNull();
        if (selectedItems.isEmpty()) {
            LOGGER.info("No selected news items found for publication");
            return List.of();
        }

        List<Publication> publications = publicationContentService.preparePublicationContent(selectedItems).stream()
                .map(Translation::getId)
                .map(this::translationWithContext)
                .flatMap(Optional::stream)
                .flatMap(translation -> publishTranslation(translation).stream())
                .toList();
        markPublicationProcessed(selectedItems);
        return publications;
    }

    private void markPublicationProcessed(List<NewsItem> selectedItems) {
        Instant processedAt = Instant.now(clock);
        transactionOperations.executeWithoutResult(status -> {
            selectedItems.forEach(newsItem -> newsItem.markPublicationProcessed(processedAt));
            newsItemRepository.saveAll(selectedItems);
        });
    }

    private Optional<Translation> translationWithContext(Long translationId) {
        if (translationId == null) {
            return Optional.empty();
        }
        return translationRepository.findWithNewsItemById(translationId);
    }

    private List<Publication> publishTranslation(Translation translation) {
        return telegramRoutingService.publicationChannelsForLanguage(translation.getTargetLanguage()).stream()
                .map(channel -> publishToChannel(translation, channel))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Publication> publishToChannel(Translation translation, TelegramChannel channel) {
        Optional<Publication> pendingPublication = transactionOperations.execute(status ->
                createPendingPublication(translation, channel));
        if (pendingPublication == null || pendingPublication.isEmpty()) {
            return Optional.empty();
        }

        Publication publication = pendingPublication.get();
        Long semanticEventId = translation.getSemanticEvent().getId();
        try (LoggingContext.Scope ignored = LoggingContext.put(Map.of(
                LogFields.SEMANTIC_EVENT_ID, String.valueOf(semanticEventId),
                LogFields.PUBLICATION_ID, String.valueOf(publication.getId())))) {
            TelegramMessageDto message = telegramMessageMapper.toMessage(translation, channel);
            TelegramSendResult result = telegramBotClient.sendMessage(channel, message);
            publication.markPublished(result.messageId(), result.messageUrl(), Instant.now(clock));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Failed to publish semantic event {} to Telegram channel {}",
                    semanticEventId,
                    channel.getCode(),
                    exception);
            publication.markFailed(exception.getMessage());
        }

        return Optional.ofNullable(transactionOperations.execute(status -> publicationRepository.save(publication)));
    }

    private Optional<Publication> createPendingPublication(Translation translation, TelegramChannel channel) {
        Long semanticEventId = translation.getSemanticEvent().getId();
        Long channelId = channel.getId();
        String targetLanguage = translation.getTargetLanguage();
        if (publicationRepository.existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
                semanticEventId,
                channelId,
                targetLanguage)) {
            return Optional.empty();
        }

        Publication publication = Publication.pending(
                translation.getSemanticEvent(),
                translation.getNewsItem(),
                translation,
                channel,
                targetLanguage);
        try {
            return Optional.of(publicationRepository.saveAndFlush(publication));
        } catch (DataIntegrityViolationException exception) {
            LOGGER.info(
                    "Skipping duplicate publication for semantic event {}, channel {}, language {}",
                    semanticEventId,
                    channelId,
                    targetLanguage);
            return Optional.empty();
        }
    }
}

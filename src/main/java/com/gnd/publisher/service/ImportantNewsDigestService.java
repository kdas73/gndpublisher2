package com.gnd.publisher.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gnd.publisher.config.ImportantNewsDigestProperties;
import com.gnd.publisher.domain.enums.PublicationStatus;
import com.gnd.publisher.domain.model.ImportantNewsDigestItem;
import com.gnd.publisher.domain.model.ImportantNewsDigestPost;
import com.gnd.publisher.domain.model.Publication;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;
import com.gnd.publisher.dto.telegram.TelegramSendResult;
import com.gnd.publisher.integration.telegram.TelegramBotClient;
import com.gnd.publisher.mapper.ImportantNewsDigestMessageMapper;
import com.gnd.publisher.repository.ImportantNewsDigestItemRepository;
import com.gnd.publisher.repository.ImportantNewsDigestPostRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.PublicationRepository;
import com.gnd.publisher.repository.SemanticEventSourceItemCount;
import com.gnd.publisher.repository.TelegramChannelRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ImportantNewsDigestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportantNewsDigestService.class);

    private final TelegramChannelRepository telegramChannelRepository;
    private final PublicationRepository publicationRepository;
    private final NewsItemRepository newsItemRepository;
    private final ImportantNewsDigestItemRepository importantNewsDigestItemRepository;
    private final ImportantNewsDigestPostRepository importantNewsDigestPostRepository;
    private final ImportantNewsDigestMessageMapper importantNewsDigestMessageMapper;
    private final TelegramBotClient telegramBotClient;
    private final ImportantNewsDigestProperties digestProperties;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    @Autowired
    public ImportantNewsDigestService(
            TelegramChannelRepository telegramChannelRepository,
            PublicationRepository publicationRepository,
            NewsItemRepository newsItemRepository,
            ImportantNewsDigestItemRepository importantNewsDigestItemRepository,
            ImportantNewsDigestPostRepository importantNewsDigestPostRepository,
            ImportantNewsDigestMessageMapper importantNewsDigestMessageMapper,
            TelegramBotClient telegramBotClient,
            ImportantNewsDigestProperties digestProperties,
            PlatformTransactionManager transactionManager) {
        this(
                telegramChannelRepository,
                publicationRepository,
                newsItemRepository,
                importantNewsDigestItemRepository,
                importantNewsDigestPostRepository,
                importantNewsDigestMessageMapper,
                telegramBotClient,
                digestProperties,
                new TransactionTemplate(transactionManager),
                Clock.systemUTC());
    }

    ImportantNewsDigestService(
            TelegramChannelRepository telegramChannelRepository,
            PublicationRepository publicationRepository,
            NewsItemRepository newsItemRepository,
            ImportantNewsDigestItemRepository importantNewsDigestItemRepository,
            ImportantNewsDigestPostRepository importantNewsDigestPostRepository,
            ImportantNewsDigestMessageMapper importantNewsDigestMessageMapper,
            TelegramBotClient telegramBotClient,
            ImportantNewsDigestProperties digestProperties,
            TransactionOperations transactionOperations,
            Clock clock) {
        this.telegramChannelRepository = telegramChannelRepository;
        this.publicationRepository = publicationRepository;
        this.newsItemRepository = newsItemRepository;
        this.importantNewsDigestItemRepository = importantNewsDigestItemRepository;
        this.importantNewsDigestPostRepository = importantNewsDigestPostRepository;
        this.importantNewsDigestMessageMapper = importantNewsDigestMessageMapper;
        this.telegramBotClient = telegramBotClient;
        this.digestProperties = digestProperties;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
    }

    public Optional<ImportantNewsDigestPost> publishImportantNewsDigest() {
        Optional<TelegramChannel> channel = telegramChannelRepository.findByCode(digestProperties.channelCode());
        if (channel.isEmpty()) {
            LOGGER.warn("Important news digest channel {} not found", digestProperties.channelCode());
            return Optional.empty();
        }
        if (!channel.get().isEnabled()) {
            LOGGER.info("Important news digest channel {} is disabled", digestProperties.channelCode());
            return Optional.empty();
        }

        List<DigestCandidate> qualifyingCandidates = qualifyingCandidates(channel.get());
        if (qualifyingCandidates.isEmpty()) {
            LOGGER.info("No qualifying semantic events found for important news digest");
            return Optional.empty();
        }

        DigestPostAndItems postAndItems = createDigestPost(channel.get(), qualifyingCandidates);
        return Optional.of(sendDigest(postAndItems, channel.get()));
    }

    private List<DigestCandidate> qualifyingCandidates(TelegramChannel channel) {
        String targetLanguage = digestProperties.targetLanguage();
        Instant since = Instant.now(clock).minus(digestProperties.lookbackWindow());
        List<Publication> candidates = publicationRepository
                .findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
                        PublicationStatus.PUBLISHED, targetLanguage, since);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, Publication> distinctBySemanticEvent = candidates.stream()
                .collect(Collectors.toMap(
                        publication -> publication.getSemanticEvent().getId(),
                        publication -> publication,
                        (first, second) -> first,
                        LinkedHashMap::new));

        List<Long> semanticEventIds = List.copyOf(distinctBySemanticEvent.keySet());
        Map<Long, Long> sourceItemCountsByEventId = newsItemRepository.countBySemanticEventIds(semanticEventIds)
                .stream()
                .collect(Collectors.toMap(
                        SemanticEventSourceItemCount::semanticEventId,
                        SemanticEventSourceItemCount::sourceItemCount));

        return distinctBySemanticEvent.values().stream()
                .map(publication -> new DigestCandidate(
                        publication,
                        sourceItemCountsByEventId.getOrDefault(publication.getSemanticEvent().getId(), 0L)))
                .filter(candidate -> candidate.sourceItemCount() > digestProperties.duplicateThreshold())
                .filter(candidate -> !importantNewsDigestItemRepository
                        .existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
                                candidate.publication().getSemanticEvent().getId(), channel.getId(), targetLanguage))
                .toList();
    }

    private DigestPostAndItems createDigestPost(TelegramChannel channel, List<DigestCandidate> qualifyingCandidates) {
        return transactionOperations.execute(status -> {
            ImportantNewsDigestPost post = importantNewsDigestPostRepository.save(ImportantNewsDigestPost.pending(
                    channel, digestProperties.targetLanguage(), digestProperties.duplicateThreshold()));
            Instant createdAt = Instant.now(clock);
            List<ImportantNewsDigestItem> items = qualifyingCandidates.stream()
                    .map(candidate -> ImportantNewsDigestItem.create(
                            post,
                            candidate.publication().getSemanticEvent(),
                            candidate.publication(),
                            channel,
                            digestProperties.targetLanguage(),
                            candidate.publication().getTranslation().getTitle(),
                            candidate.publication().getTelegramMessageUrl(),
                            Math.toIntExact(candidate.sourceItemCount()),
                            createdAt))
                    .toList();
            importantNewsDigestItemRepository.saveAll(items);
            return new DigestPostAndItems(post, items);
        });
    }

    private ImportantNewsDigestPost sendDigest(DigestPostAndItems postAndItems, TelegramChannel channel) {
        ImportantNewsDigestPost post = postAndItems.post();
        TelegramMessageDto message = importantNewsDigestMessageMapper.toMessage(postAndItems.items(), channel);
        try {
            TelegramSendResult result = telegramBotClient.sendMessage(channel, message);
            post.markPublished(result.messageId(), result.messageUrl(), Instant.now(clock));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to publish important news digest to Telegram channel {}",
                    channel.getCode(), exception);
            post.markFailed(exception.getMessage());
        }
        return transactionOperations.execute(status -> importantNewsDigestPostRepository.save(post));
    }

    private record DigestCandidate(Publication publication, long sourceItemCount) {
    }

    private record DigestPostAndItems(ImportantNewsDigestPost post, List<ImportantNewsDigestItem> items) {
    }
}

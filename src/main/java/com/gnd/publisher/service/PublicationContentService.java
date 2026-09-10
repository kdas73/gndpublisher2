package com.gnd.publisher.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import com.gnd.publisher.config.PublishingProperties;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.NewsItemCategory;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.llm.PublicationContentRequest;
import com.gnd.publisher.dto.llm.PublicationContentResponse;
import com.gnd.publisher.integration.llm.LlmPublicationContentGenerator;
import com.gnd.publisher.repository.NewsItemCategoryRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.TranslationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PublicationContentService {

    private final LlmPublicationContentGenerator generator;
    private final TranslationRepository translationRepository;
    private final NewsItemRepository newsItemRepository;
    private final NewsItemCategoryRepository newsItemCategoryRepository;
    private final PublishingProperties publishingProperties;
    private final TransactionOperations transactionOperations;

    @Autowired
    public PublicationContentService(
            LlmPublicationContentGenerator generator,
            TranslationRepository translationRepository,
            NewsItemRepository newsItemRepository,
            NewsItemCategoryRepository newsItemCategoryRepository,
            PublishingProperties publishingProperties,
            PlatformTransactionManager transactionManager) {
        this(
                generator,
                translationRepository,
                newsItemRepository,
                newsItemCategoryRepository,
                publishingProperties,
                new TransactionTemplate(transactionManager));
    }

    PublicationContentService(
            LlmPublicationContentGenerator generator,
            TranslationRepository translationRepository,
            NewsItemRepository newsItemRepository,
            NewsItemCategoryRepository newsItemCategoryRepository,
            PublishingProperties publishingProperties,
            TransactionOperations transactionOperations) {
        this.generator = generator;
        this.translationRepository = translationRepository;
        this.newsItemRepository = newsItemRepository;
        this.newsItemCategoryRepository = newsItemCategoryRepository;
        this.publishingProperties = publishingProperties;
        this.transactionOperations = transactionOperations;
    }

    public List<Translation> preparePublicationContent(Collection<NewsItem> selectedItems) {
        return selectedItems.stream()
                .filter(NewsItem::isSelectedForPublication)
                .flatMap(newsItem -> publishingProperties.targetLanguages().stream()
                        .map(targetLanguage -> prepareForLanguage(newsItem, targetLanguage))
                        .flatMap(Optional::stream))
                .toList();
    }

    private NewsItem publicationContext(NewsItem newsItem) {
        return Optional.ofNullable(newsItem.getId())
                .flatMap(newsItemRepository::findPublicationContentContextById)
                .filter(item -> item.getSemanticEvent() != null)
                .orElse(newsItem);
    }

    private Optional<Translation> prepareForLanguage(NewsItem newsItem, String targetLanguage) {
        PublicationContentPlan plan = transactionOperations.execute(status -> contentPlan(newsItem, targetLanguage));
        if (plan == null) {
            return Optional.empty();
        }
        if (plan.existingTranslation() != null) {
            return Optional.of(plan.existingTranslation());
        }
        LlmPublicationContentGenerator.PublicationContentResult result =
                generator.prepare(Objects.requireNonNull(plan.request()));
        return Optional.of(saveGeneratedTranslation(plan, result));
    }

    private PublicationContentPlan contentPlan(NewsItem selectedItem, String targetLanguage) {
        NewsItem newsItem = publicationContext(selectedItem);
        SemanticNewsEvent semanticEvent = newsItem.getSemanticEvent();
        if (semanticEvent == null) {
            return null;
        }
        Optional<Translation> existingTranslation = translationRepository
                .findBySemanticEvent_IdAndTargetLanguage(semanticEvent.getId(), targetLanguage);
        if (existingTranslation.isPresent()) {
            return new PublicationContentPlan(existingTranslation.get(), null, null, targetLanguage, null);
        }
        return new PublicationContentPlan(
                null,
                newsItem,
                semanticEvent,
                targetLanguage,
                request(newsItem, semanticEvent, targetLanguage));
    }

    private Translation saveGeneratedTranslation(
            PublicationContentPlan plan,
            LlmPublicationContentGenerator.PublicationContentResult result) {
        PublicationContentResponse response = result.response();
        return Objects.requireNonNull(transactionOperations.execute(status -> {
            Translation translation = Translation.publicationContent(
                    plan.semanticEvent(),
                    plan.newsItem(),
                    plan.targetLanguage(),
                    response.title(),
                    response.summary(),
                    result.providerId(),
                    result.model());
            return translationRepository.save(translation);
        }));
    }

    private PublicationContentRequest request(
            NewsItem newsItem,
            SemanticNewsEvent semanticEvent,
            String targetLanguage) {
        List<String> categoryCodes = categoryCodes(newsItem, semanticEvent);
        return new PublicationContentRequest(
                newsItem.getTitle(),
                newsItem.getSummary(),
                newsItem.getSource().getName(),
                newsItem.getSourceUrl(),
                newsItem.getOriginalLanguage(),
                targetLanguage,
                semanticEvent.getSemanticKey(),
                primaryCategoryCode(semanticEvent),
                categoryCodes,
                publishingProperties.summaryMaxCharacters(),
                publishingProperties.telegramMaxMessageCharacters());
    }

    private List<String> categoryCodes(NewsItem newsItem, SemanticNewsEvent semanticEvent) {
        List<String> categoryCodes = newsItemCategoryRepository.findByNewsItem_Id(newsItem.getId()).stream()
                .map(NewsItemCategory::getCategory)
                .map(Category::getCode)
                .distinct()
                .toList();
        if (!categoryCodes.isEmpty()) {
            return categoryCodes;
        }
        return List.of(primaryCategoryCode(semanticEvent));
    }

    private String primaryCategoryCode(SemanticNewsEvent semanticEvent) {
        return Optional.ofNullable(semanticEvent.getCategory())
                .map(Category::getCode)
                .orElseThrow(() -> new IllegalStateException("Semantic event has no primary category"));
    }

    private record PublicationContentPlan(
            Translation existingTranslation,
            NewsItem newsItem,
            SemanticNewsEvent semanticEvent,
            String targetLanguage,
            PublicationContentRequest request) {
    }
}

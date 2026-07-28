package com.gnd.publisher.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gnd.publisher.config.CategoryProperties;
import com.gnd.publisher.config.OpenAiProperties;
import com.gnd.publisher.domain.enums.RejectionReason;
import com.gnd.publisher.domain.enums.SemanticKeyAction;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.ClassificationRun;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.NewsItemCategory;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.CategoryOptionDto;
import com.gnd.publisher.dto.openai.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.openai.OpenAiNewsItemDto;
import com.gnd.publisher.dto.openai.SemanticKeyActionDto;
import com.gnd.publisher.exception.OpenAiIntegrationException;
import com.gnd.publisher.integration.openai.OpenAiCategorizer;
import com.gnd.publisher.integration.openai.PromptLoader;
import com.gnd.publisher.logging.LogFields;
import com.gnd.publisher.logging.LoggingContext;
import com.gnd.publisher.repository.CategoryRepository;
import com.gnd.publisher.repository.ClassificationRunRepository;
import com.gnd.publisher.repository.NewsItemCategoryRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.service.SemanticEventGroupingService.CandidateSemanticEvents;
import com.gnd.publisher.service.SemanticEventGroupingService.GroupedSemanticEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CategorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategorizationService.class);

    private final OpenAiCategorizer openAiCategorizer;
    private final SemanticEventGroupingService semanticEventGroupingService;
    private final CategoryRepository categoryRepository;
    private final NewsItemRepository newsItemRepository;
    private final NewsItemCategoryRepository newsItemCategoryRepository;
    private final ClassificationRunRepository classificationRunRepository;
    private final CategoryProperties categoryProperties;
    private final OpenAiProperties openAiProperties;
    private final PromptLoader promptLoader;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    @Autowired
    public CategorizationService(
            OpenAiCategorizer openAiCategorizer,
            SemanticEventGroupingService semanticEventGroupingService,
            CategoryRepository categoryRepository,
            NewsItemRepository newsItemRepository,
            NewsItemCategoryRepository newsItemCategoryRepository,
            ClassificationRunRepository classificationRunRepository,
            CategoryProperties categoryProperties,
            OpenAiProperties openAiProperties,
            PromptLoader promptLoader,
            PlatformTransactionManager transactionManager) {
        this(
                openAiCategorizer,
                semanticEventGroupingService,
                categoryRepository,
                newsItemRepository,
                newsItemCategoryRepository,
                classificationRunRepository,
                categoryProperties,
                openAiProperties,
                promptLoader,
                new TransactionTemplate(transactionManager),
                Clock.systemUTC());
    }

    CategorizationService(
            OpenAiCategorizer openAiCategorizer,
            SemanticEventGroupingService semanticEventGroupingService,
            CategoryRepository categoryRepository,
            NewsItemRepository newsItemRepository,
            NewsItemCategoryRepository newsItemCategoryRepository,
            ClassificationRunRepository classificationRunRepository,
            CategoryProperties categoryProperties,
            OpenAiProperties openAiProperties,
            PromptLoader promptLoader,
            TransactionOperations transactionOperations,
            Clock clock) {
        this.openAiCategorizer = openAiCategorizer;
        this.semanticEventGroupingService = semanticEventGroupingService;
        this.categoryRepository = categoryRepository;
        this.newsItemRepository = newsItemRepository;
        this.newsItemCategoryRepository = newsItemCategoryRepository;
        this.classificationRunRepository = classificationRunRepository;
        this.categoryProperties = categoryProperties;
        this.openAiProperties = openAiProperties;
        this.promptLoader = promptLoader;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
    }

    public void classifyNewItems(Collection<NewsItem> newsItems) {
        if (newsItems.isEmpty()) {
            return;
        }

        Map<String, Category> categoriesByCode = Objects.requireNonNull(
                transactionOperations.execute(status -> syncConfiguredCategories()));
        List<CategoryOptionDto> categoryOptions = categoryOptions();
        List<String> editorialRules = editorialRules();
        newsItems.forEach(newsItem -> classifyOne(newsItem, categoriesByCode, categoryOptions, editorialRules));
    }

    private void classifyOne(
            NewsItem newsItem,
            Map<String, Category> categoriesByCode,
            List<CategoryOptionDto> categoryOptions,
            List<String> editorialRules) {
        Instant classifiedAt = Instant.now(clock);
        try (LoggingContext.Scope ignored =
                LoggingContext.put(LogFields.NEWS_ITEM_ID, String.valueOf(newsItem.getId()))) {
            CandidateSemanticEvents candidates = semanticEventGroupingService.recentCandidates();
            CategoryClassificationRequest request = new CategoryClassificationRequest(
                    newsItemDto(newsItem),
                    categoryOptions,
                    categoryProperties.publishableCodes(),
                    editorialRules,
                    candidates.dtos());
            OpenAiCategorizer.CategorizationResult result = openAiCategorizer.classify(request);
            CategoryClassificationResponse response = result.response();
            transactionOperations.executeWithoutResult(status ->
                    saveSuccessfulClassification(newsItem, categoriesByCode, candidates, result, response, classifiedAt));
        } catch (RuntimeException exception) {
            transactionOperations.executeWithoutResult(status -> {
                newsItem.markClassificationFailed(classifiedAt);
                newsItemRepository.save(newsItem);
            });
            LOGGER.warn("Failed to classify news item {}", newsItem.getId(), exception);
        }
    }

    private void saveSuccessfulClassification(
            NewsItem newsItem,
            Map<String, Category> categoriesByCode,
            CandidateSemanticEvents candidates,
            OpenAiCategorizer.CategorizationResult result,
            CategoryClassificationResponse response,
            Instant classifiedAt) {
        validateResponseCategories(response, categoriesByCode.keySet());

        Category primaryCategory = category(response.primaryCategoryCode(), categoriesByCode);
        GroupedSemanticEvent groupedEvent = semanticEventGroupingService.group(
                response.semanticKeyAction(),
                response.semanticKey(),
                response.matchedSemanticEventId(),
                candidates,
                primaryCategory,
                classifiedAt);

        BigDecimal confidence = BigDecimal.valueOf(response.confidence());
        RejectionReason rejectionReason = rejectionReason(response);
        newsItem.markClassified(
                groupedEvent.semanticEvent(),
                confidence,
                response.shouldPublish(),
                rejectionReason,
                classifiedAt);
        newsItemRepository.save(newsItem);
        saveCategoryMatches(newsItem, response.categoryCodes(), categoriesByCode, result.model(), confidence, classifiedAt);
        saveClassificationRun(
                newsItem,
                groupedEvent.semanticEvent(),
                groupedEvent.matchedSemanticEvent(),
                primaryCategory,
                candidates,
                result,
                confidence,
                classifiedAt);
    }

    private Map<String, Category> syncConfiguredCategories() {
        Set<String> publishableCodes = Set.copyOf(categoryProperties.publishableCodes());
        return categoryProperties.options().stream()
                .map(option -> syncCategory(option, publishableCodes.contains(option.code())))
                .collect(Collectors.toMap(Category::getCode, Function.identity()));
    }

    private Category syncCategory(CategoryProperties.Category option, boolean publishable) {
        Category category = categoryRepository.findByCode(option.code())
                .map(existing -> {
                    existing.updateFromConfiguration(
                            option.name(),
                            option.description(),
                            option.enabled(),
                            publishable,
                            option.publicationPriority());
                    return existing;
                })
                .orElseGet(() -> Category.create(
                        option.code(),
                        option.name(),
                        option.description(),
                        option.enabled(),
                        publishable,
                        option.publicationPriority()));
        return categoryRepository.save(category);
    }

    private List<CategoryOptionDto> categoryOptions() {
        return categoryProperties.options().stream()
                .filter(CategoryProperties.Category::enabled)
                .map(option -> new CategoryOptionDto(option.code(), option.description()))
                .toList();
    }

    private List<String> editorialRules() {
        return promptLoader.load(openAiProperties.prompts().editorialRulesVersion()).lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private OpenAiNewsItemDto newsItemDto(NewsItem newsItem) {
        return new OpenAiNewsItemDto(
                newsItem.getTitle(),
                newsItem.getSummary(),
                newsItem.getSource().getName(),
                newsItem.getSourceUrl(),
                newsItem.getPublishedAt() == null ? newsItem.getFetchedAt() : newsItem.getPublishedAt());
    }

    private void validateResponseCategories(CategoryClassificationResponse response, Set<String> categoryCodes) {
        if (response.semanticKeyAction() == null) {
            throw new OpenAiIntegrationException("Classification semanticKeyAction is required");
        }
        if (!categoryCodes.contains(response.primaryCategoryCode())) {
            throw new OpenAiIntegrationException("Classification primaryCategoryCode is not configured: "
                    + response.primaryCategoryCode());
        }
        if (response.categoryCodes().stream().anyMatch(code -> !categoryCodes.contains(code))) {
            throw new OpenAiIntegrationException("Classification categoryCodes contain an unconfigured category");
        }
    }

    private Category category(String code, Map<String, Category> categoriesByCode) {
        return java.util.Optional.ofNullable(categoriesByCode.get(code))
                .orElseThrow(() -> new OpenAiIntegrationException("Category not configured: " + code));
    }

    private RejectionReason rejectionReason(CategoryClassificationResponse response) {
        if (response.shouldPublish()) {
            return null;
        }
        if (response.rejectionReason() == null) {
            return RejectionReason.NOT_PUBLISHABLE_CATEGORY;
        }
        return mapRejectionReason(response.rejectionReason());
    }

    private RejectionReason mapRejectionReason(ClassificationRejectionReasonDto rejectionReason) {
        return RejectionReason.valueOf(rejectionReason.name());
    }

    private void saveCategoryMatches(
            NewsItem newsItem,
            List<String> categoryCodes,
            Map<String, Category> categoriesByCode,
            String model,
            BigDecimal confidence,
            Instant createdAt) {
        newsItemCategoryRepository.deleteByNewsItem_Id(newsItem.getId());
        List<NewsItemCategory> matches = categoryCodes.stream()
                .distinct()
                .map(code -> NewsItemCategory.classifierMatch(
                        newsItem,
                        category(code, categoriesByCode),
                        model,
                        confidence,
                        createdAt))
                .toList();
        newsItemCategoryRepository.saveAll(matches);
    }

    private void saveClassificationRun(
            NewsItem newsItem,
            SemanticNewsEvent semanticEvent,
            SemanticNewsEvent matchedSemanticEvent,
            Category primaryCategory,
            CandidateSemanticEvents candidates,
            OpenAiCategorizer.CategorizationResult result,
            BigDecimal confidence,
            Instant createdAt) {
        CategoryClassificationResponse response = result.response();
        classificationRunRepository.save(ClassificationRun.recordDecision(
                newsItem,
                semanticEvent,
                result.model(),
                candidates.dtos().size(),
                candidates.lookupWindowStartedAt(),
                candidates.lookupWindowEndedAt(),
                openAiProperties.prompts().editorialRulesVersion(),
                response.semanticKey(),
                SemanticKeyAction.valueOf(response.semanticKeyAction().name()),
                matchedSemanticEvent,
                primaryCategory,
                confidence,
                result.rawResponse(),
                createdAt));
    }
}

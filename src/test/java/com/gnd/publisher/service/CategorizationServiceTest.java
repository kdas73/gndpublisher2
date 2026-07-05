package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gnd.publisher.config.CategoryProperties;
import com.gnd.publisher.config.OpenAiProperties;
import com.gnd.publisher.domain.enums.ClassificationStatus;
import com.gnd.publisher.domain.enums.RejectionReason;
import com.gnd.publisher.domain.enums.SemanticKeyAction;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.ClassificationRun;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.NewsItemCategory;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.ClassificationRejectionReasonDto;
import com.gnd.publisher.dto.openai.SemanticEventKeyCandidateDto;
import com.gnd.publisher.dto.openai.SemanticKeyActionDto;
import com.gnd.publisher.integration.openai.OpenAiCategorizer;
import com.gnd.publisher.integration.openai.PromptLoader;
import com.gnd.publisher.repository.CategoryRepository;
import com.gnd.publisher.repository.ClassificationRunRepository;
import com.gnd.publisher.repository.NewsItemCategoryRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.service.SemanticEventGroupingService.CandidateSemanticEvents;
import com.gnd.publisher.service.SemanticEventGroupingService.GroupedSemanticEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private OpenAiCategorizer openAiCategorizer;

    @Mock
    private SemanticEventGroupingService semanticEventGroupingService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private NewsItemCategoryRepository newsItemCategoryRepository;

    @Mock
    private ClassificationRunRepository classificationRunRepository;

    @Mock
    private PromptLoader promptLoader;

    @Test
    void classifiesPublishableItemAndCreatesSemanticEvent() {
        Category politics = configuredCategory("politics", 10, true);
        mockCategorySync(politics);
        NewsItem newsItem = newsItem(100L);
        CandidateSemanticEvents candidates = emptyCandidates();
        SemanticNewsEvent createdEvent = semanticEvent(200L, "greek parliament approves migration bill", politics);
        CategoryClassificationResponse response = response(
                "politics",
                List.of("politics"),
                SemanticKeyActionDto.CREATED_NEW,
                null,
                true,
                null);
        when(semanticEventGroupingService.recentCandidates()).thenReturn(candidates);
        when(openAiCategorizer.classify(any(CategoryClassificationRequest.class)))
                .thenReturn(result(response));
        when(semanticEventGroupingService.group(
                SemanticKeyActionDto.CREATED_NEW,
                response.semanticKey(),
                null,
                candidates,
                politics,
                NOW))
                .thenReturn(new GroupedSemanticEvent(createdEvent, null));

        service().classifyNewItems(List.of(newsItem));

        assertThat(newsItem.getClassificationStatus()).isEqualTo(ClassificationStatus.CLASSIFIED);
        assertThat(newsItem.getSemanticEvent()).isSameAs(createdEvent);
        assertThat(newsItem.isPublicationCandidate()).isTrue();
        assertThat(newsItem.getRejectionReason()).isNull();
        assertThat(newsItem.getClassificationConfidence()).isEqualByComparingTo("0.91");

        ArgumentCaptor<CategoryClassificationRequest> requestCaptor =
                ArgumentCaptor.forClass(CategoryClassificationRequest.class);
        verify(openAiCategorizer).classify(requestCaptor.capture());
        assertThat(requestCaptor.getValue().newsItem().publishedAt()).isEqualTo("2026-07-01T10:00:00Z");
        assertThat(requestCaptor.getValue().editorialRules())
                .containsExactly("Rule one.", "Rule two.");

        ArgumentCaptor<ClassificationRun> runCaptor = ArgumentCaptor.forClass(ClassificationRun.class);
        verify(classificationRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getSemanticKeyAction()).isEqualTo(SemanticKeyAction.CREATED_NEW);
        assertThat(runCaptor.getValue().getRawResponse()).contains("semanticKey");
        assertThat(runCaptor.getValue().getEditorialRulesVersion()).isEqualTo("editorial-rules-v1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NewsItemCategory>> categoryCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(newsItemCategoryRepository).saveAll(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue()).singleElement()
                .satisfies(match -> assertThat(match.getCategory()).isSameAs(politics));
    }

    @Test
    void classifiesItemAgainstMatchedExistingSemanticEvent() {
        Category politics = configuredCategory("politics", 10, true);
        mockCategorySync(politics);
        NewsItem newsItem = newsItem(100L);
        SemanticNewsEvent matchedEvent = semanticEvent(200L, "greek parliament debates migration bill", politics);
        CandidateSemanticEvents candidates = new CandidateSemanticEvents(
                List.of(new SemanticEventKeyCandidateDto("200", matchedEvent.getSemanticKey())),
                Map.of("200", matchedEvent),
                NOW.minus(Duration.ofDays(3)),
                NOW);
        CategoryClassificationResponse response = response(
                "politics",
                List.of("politics"),
                SemanticKeyActionDto.MATCHED_EXISTING,
                "200",
                true,
                null);
        when(semanticEventGroupingService.recentCandidates()).thenReturn(candidates);
        when(openAiCategorizer.classify(any(CategoryClassificationRequest.class)))
                .thenReturn(result(response));
        when(semanticEventGroupingService.group(
                SemanticKeyActionDto.MATCHED_EXISTING,
                response.semanticKey(),
                "200",
                candidates,
                politics,
                NOW))
                .thenReturn(new GroupedSemanticEvent(matchedEvent, matchedEvent));

        service().classifyNewItems(List.of(newsItem));

        ArgumentCaptor<ClassificationRun> runCaptor = ArgumentCaptor.forClass(ClassificationRun.class);
        verify(classificationRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getSemanticKeyAction()).isEqualTo(SemanticKeyAction.MATCHED_EXISTING);
        assertThat(runCaptor.getValue().getMatchedSemanticEvent()).isSameAs(matchedEvent);
        assertThat(runCaptor.getValue().getInputKeysCount()).isEqualTo(1);
    }

    @Test
    void marksEditorialRuleExclusionAsRejected() {
        Category politics = configuredCategory("politics", 10, true);
        mockCategorySync(politics);
        NewsItem newsItem = newsItem(100L);
        SemanticNewsEvent createdEvent = semanticEvent(200L, "celebrity death without public impact", politics);
        CandidateSemanticEvents candidates = emptyCandidates();
        CategoryClassificationResponse response = response(
                "politics",
                List.of("politics"),
                SemanticKeyActionDto.CREATED_NEW,
                null,
                false,
                ClassificationRejectionReasonDto.EDITORIAL_RULE_EXCLUDED);
        when(semanticEventGroupingService.recentCandidates()).thenReturn(candidates);
        when(openAiCategorizer.classify(any(CategoryClassificationRequest.class)))
                .thenReturn(result(response));
        when(semanticEventGroupingService.group(
                SemanticKeyActionDto.CREATED_NEW,
                response.semanticKey(),
                null,
                candidates,
                politics,
                NOW))
                .thenReturn(new GroupedSemanticEvent(createdEvent, null));

        service().classifyNewItems(List.of(newsItem));

        assertThat(newsItem.getClassificationStatus()).isEqualTo(ClassificationStatus.REJECTED);
        assertThat(newsItem.isPublicationCandidate()).isFalse();
        assertThat(newsItem.getRejectionReason()).isEqualTo(RejectionReason.EDITORIAL_RULE_EXCLUDED);
    }

    @Test
    void marksInvalidCategoryCodeAsClassificationFailed() {
        Category politics = configuredCategory("politics", 10, true);
        mockCategorySync(politics);
        NewsItem newsItem = newsItem(100L);
        when(semanticEventGroupingService.recentCandidates()).thenReturn(emptyCandidates());
        when(openAiCategorizer.classify(any(CategoryClassificationRequest.class)))
                .thenReturn(result(response(
                        "sports",
                        List.of("sports"),
                        SemanticKeyActionDto.CREATED_NEW,
                        null,
                        false,
                        ClassificationRejectionReasonDto.NOT_PUBLISHABLE_CATEGORY)));

        service().classifyNewItems(List.of(newsItem));

        assertThat(newsItem.getClassificationStatus()).isEqualTo(ClassificationStatus.FAILED);
        assertThat(newsItem.getRejectionReason()).isEqualTo(RejectionReason.CLASSIFICATION_FAILED);
        verify(classificationRunRepository, never()).save(any());
    }

    @Test
    void marksInvalidSemanticKeyActionAsClassificationFailed() {
        Category politics = configuredCategory("politics", 10, true);
        mockCategorySync(politics);
        NewsItem newsItem = newsItem(100L);
        when(semanticEventGroupingService.recentCandidates()).thenReturn(emptyCandidates());
        when(openAiCategorizer.classify(any(CategoryClassificationRequest.class)))
                .thenReturn(result(response(
                        "politics",
                        List.of("politics"),
                        null,
                        null,
                        true,
                        null)));

        service().classifyNewItems(List.of(newsItem));

        assertThat(newsItem.getClassificationStatus()).isEqualTo(ClassificationStatus.FAILED);
        assertThat(newsItem.getRejectionReason()).isEqualTo(RejectionReason.CLASSIFICATION_FAILED);
        verify(semanticEventGroupingService, never()).group(any(), any(), any(), any(), any(), any());
    }

    private void mockCategorySync(Category politics) {
        when(categoryRepository.findByCode("politics")).thenReturn(Optional.of(politics));
        when(categoryRepository.save(politics)).thenReturn(politics);
        when(promptLoader.load("editorial-rules-v1")).thenReturn("""
                Rule one.
                
                Rule two.
                """);
    }

    private CategorizationService service() {
        return new CategorizationService(
                openAiCategorizer,
                semanticEventGroupingService,
                categoryRepository,
                newsItemRepository,
                newsItemCategoryRepository,
                classificationRunRepository,
                categoryProperties(),
                openAiProperties(),
                promptLoader,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private CategoryProperties categoryProperties() {
        return new CategoryProperties(
                List.of(new CategoryProperties.Category(
                        "politics",
                        "Politics",
                        "Government, elections, laws, public administration",
                        10,
                        true)),
                List.of("politics"));
    }

    private OpenAiProperties openAiProperties() {
        return new OpenAiProperties(
                "test-key",
                new OpenAiProperties.Models("GPT5.5-mini", "GPT-5.5"),
                new OpenAiProperties.Prompts(
                        "classification-v1",
                        "editorial-rules-v1",
                        "publication-content-v1"),
                new OpenAiProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60)),
                Duration.ofDays(3));
    }

    private NewsItem newsItem(Long id) {
        RssSource source = RssSource.create("ert-news", "ERT News", "https://feeds.example.test/ert", "el", true);
        ReflectionTestUtils.setField(source, "id", 10L);
        NewsItem newsItem = NewsItem.fromRssFeedItem(
                source,
                new com.gnd.publisher.dto.rss.RssFeedItemDto(
                        Optional.of("external-1"),
                        "Greek parliament approves new migration bill",
                        Optional.of("https://example.test/news/item"),
                        Optional.of("Greek lawmakers approved a new migration bill after debate."),
                        Optional.of(Instant.parse("2026-07-01T10:00:00Z"))),
                NOW);
        ReflectionTestUtils.setField(newsItem, "id", id);
        return newsItem;
    }

    private Category configuredCategory(String code, int priority, boolean publishable) {
        Category category = Category.create(code, "Politics", "Government, elections", true, publishable, priority);
        ReflectionTestUtils.setField(category, "id", 1L);
        return category;
    }

    private SemanticNewsEvent semanticEvent(Long id, String semanticKey, Category category) {
        SemanticNewsEvent event = SemanticNewsEvent.create(semanticKey, category, NOW);
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private CandidateSemanticEvents emptyCandidates() {
        return new CandidateSemanticEvents(List.of(), Map.of(), NOW.minus(Duration.ofDays(3)), NOW);
    }

    private CategoryClassificationResponse response(
            String primaryCategoryCode,
            List<String> categoryCodes,
            SemanticKeyActionDto action,
            String matchedSemanticEventId,
            boolean shouldPublish,
            ClassificationRejectionReasonDto rejectionReason) {
        return new CategoryClassificationResponse(
                primaryCategoryCode,
                categoryCodes,
                "greek parliament approves migration bill",
                action,
                matchedSemanticEventId,
                0.91,
                shouldPublish,
                rejectionReason);
    }

    private OpenAiCategorizer.CategorizationResult result(CategoryClassificationResponse response) {
        return new OpenAiCategorizer.CategorizationResult(response, "{\"semanticKey\":\"test\"}", "GPT5.5-mini");
    }
}

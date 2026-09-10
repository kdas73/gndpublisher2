package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gnd.publisher.config.PublishingProperties;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.NewsItemCategory;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.llm.PublicationContentRequest;
import com.gnd.publisher.dto.llm.PublicationContentResponse;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.integration.llm.LlmPublicationContentGenerator;
import com.gnd.publisher.repository.NewsItemCategoryRepository;
import com.gnd.publisher.repository.NewsItemRepository;
import com.gnd.publisher.repository.TranslationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class PublicationContentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private LlmPublicationContentGenerator generator;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private NewsItemCategoryRepository newsItemCategoryRepository;

    @Test
    void generatesPublicationContentForSelectedItem() {
        Category politics = category(1L, "politics");
        NewsItem newsItem = selectedNewsItem(100L, politics);
        when(newsItemRepository.findPublicationContentContextById(100L)).thenReturn(Optional.of(newsItem));
        when(translationRepository.findBySemanticEvent_IdAndTargetLanguage(200L, "en"))
                .thenReturn(Optional.empty());
        when(newsItemCategoryRepository.findByNewsItem_Id(100L))
                .thenReturn(List.of(NewsItemCategory.classifierMatch(
                        newsItem,
                        politics,
                        "openai",
                        "gpt-5.4-mini",
                        java.math.BigDecimal.valueOf(0.91),
                        NOW)));
        when(generator.prepare(any(PublicationContentRequest.class)))
                .thenReturn(new LlmPublicationContentGenerator.PublicationContentResult(
                        new PublicationContentResponse(
                                "Greek parliament approves new migration bill",
                                "Greek lawmakers approved a new migration bill after debate.",
                                0.9),
                        "translategemma:4b",
                        "ollama"));
        when(translationRepository.save(any(Translation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Translation> translations = service("en").preparePublicationContent(List.of(newsItem));

        assertThat(translations).singleElement()
                .satisfies(translation -> {
                    assertThat(translation.getSemanticEvent()).isSameAs(newsItem.getSemanticEvent());
                    assertThat(translation.getNewsItem()).isSameAs(newsItem);
                    assertThat(translation.getTargetLanguage()).isEqualTo("en");
                    // The provider and model that actually produced the content, not a literal.
                    assertThat(translation.getProvider()).isEqualTo("ollama");
                    assertThat(translation.getModel()).isEqualTo("translategemma:4b");
                });

        ArgumentCaptor<PublicationContentRequest> requestCaptor =
                ArgumentCaptor.forClass(PublicationContentRequest.class);
        verify(generator).prepare(requestCaptor.capture());
        assertThat(requestCaptor.getValue().sourceName()).isEqualTo("ERT News");
        assertThat(requestCaptor.getValue().sourceLanguage()).isEqualTo("el");
        assertThat(requestCaptor.getValue().targetLanguage()).isEqualTo("en");
        assertThat(requestCaptor.getValue().semanticKey()).isEqualTo("greek parliament approves migration bill");
        assertThat(requestCaptor.getValue().primaryCategoryCode()).isEqualTo("politics");
        assertThat(requestCaptor.getValue().categoryCodes()).containsExactly("politics");
        assertThat(requestCaptor.getValue().maxSummaryCharacters()).isEqualTo(600);
        assertThat(requestCaptor.getValue().maxMessageCharacters()).isEqualTo(3500);
    }

    @Test
    void reusesExistingTranslationForSemanticEventAndLanguage() {
        Category politics = category(1L, "politics");
        NewsItem newsItem = selectedNewsItem(100L, politics);
        when(newsItemRepository.findPublicationContentContextById(100L)).thenReturn(Optional.of(newsItem));
        Translation existing = Translation.publicationContent(
                newsItem.getSemanticEvent(),
                newsItem,
                "en",
                "Existing title",
                "Existing summary",
                "openai",
                "gpt-5.5");
        when(translationRepository.findBySemanticEvent_IdAndTargetLanguage(200L, "en"))
                .thenReturn(Optional.of(existing));

        List<Translation> translations = service("en").preparePublicationContent(List.of(newsItem));

        assertThat(translations).containsExactly(existing);
        verify(generator, never()).prepare(any());
        verify(translationRepository, never()).save(any());
    }

    @Test
    void preparesContentForMultipleTargetLanguages() {
        Category politics = category(1L, "politics");
        NewsItem newsItem = selectedNewsItem(100L, politics);
        when(newsItemRepository.findPublicationContentContextById(100L)).thenReturn(Optional.of(newsItem));
        when(translationRepository.findBySemanticEvent_IdAndTargetLanguage(200L, "en"))
                .thenReturn(Optional.empty());
        when(translationRepository.findBySemanticEvent_IdAndTargetLanguage(200L, "fr"))
                .thenReturn(Optional.empty());
        when(newsItemCategoryRepository.findByNewsItem_Id(100L)).thenReturn(List.of());
        when(generator.prepare(any(PublicationContentRequest.class)))
                .thenAnswer(invocation -> {
                    PublicationContentRequest request = invocation.getArgument(0);
                    return new LlmPublicationContentGenerator.PublicationContentResult(
                            new PublicationContentResponse(
                                    "Title " + request.targetLanguage(),
                                    "Summary " + request.targetLanguage(),
                                    0.9),
                            "translategemma:4b",
                            "ollama");
                });
        when(translationRepository.save(any(Translation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Translation> translations = service("en", "fr").preparePublicationContent(List.of(newsItem));

        assertThat(translations).extracting(Translation::getTargetLanguage)
                .containsExactly("en", "fr");
        verify(generator, org.mockito.Mockito.times(2)).prepare(any());
    }

    @Test
    void skipsItemsThatWereNotSelectedForPublication() {
        Category politics = category(1L, "politics");
        NewsItem newsItem = selectedNewsItem(100L, politics);
        ReflectionTestUtils.setField(newsItem, "selectedForPublication", false);

        List<Translation> translations = service("en").preparePublicationContent(List.of(newsItem));

        assertThat(translations).isEmpty();
        verify(generator, never()).prepare(any());
        verify(translationRepository, never()).save(any());
    }

    private PublicationContentService service(String... targetLanguages) {
        return new PublicationContentService(
                generator,
                translationRepository,
                newsItemRepository,
                newsItemCategoryRepository,
                publishingProperties(targetLanguages),
                transactionOperations());
    }

    private TransactionOperations transactionOperations() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }

    private PublishingProperties publishingProperties(String... targetLanguages) {
        return new PublishingProperties(
                new LinkedHashSet<>(List.of(targetLanguages)),
                3,
                Map.of(),
                600,
                3500,
                new PublishingProperties.ImportantNewsDigest(true, 2, "important-news"));
    }

    private NewsItem selectedNewsItem(Long id, Category category) {
        RssSource source = RssSource.create(
                "ert-news",
                "ERT News",
                "https://feeds.example.test/ert",
                "el",
                true);
        ReflectionTestUtils.setField(source, "id", 10L);
        NewsItem newsItem = NewsItem.fromRssFeedItem(
                source,
                new RssFeedItemDto(
                        Optional.of("external-1"),
                        "Greek parliament approves new migration bill",
                        Optional.of("https://example.test/news/item"),
                        Optional.of("Greek lawmakers approved a new migration bill after debate."),
                        Optional.of(Instant.parse("2026-07-01T10:00:00Z"))),
                NOW);
        ReflectionTestUtils.setField(newsItem, "id", id);
        SemanticNewsEvent semanticEvent = SemanticNewsEvent.create(
                "greek parliament approves migration bill",
                category,
                NOW);
        ReflectionTestUtils.setField(semanticEvent, "id", 200L);
        newsItem.markClassified(semanticEvent, java.math.BigDecimal.valueOf(0.91), true, null, NOW);
        newsItem.markSelectedForPublication();
        return newsItem;
    }

    private Category category(Long id, String code) {
        Category category = Category.create(code, "Politics", "Government", true, true, 10);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}

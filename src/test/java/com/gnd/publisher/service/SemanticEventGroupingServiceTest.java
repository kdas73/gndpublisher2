package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.gnd.publisher.config.OpenAiProperties;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.dto.openai.SemanticKeyActionDto;
import com.gnd.publisher.repository.SemanticNewsEventRepository;
import com.gnd.publisher.service.SemanticEventGroupingService.CandidateSemanticEvents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SemanticEventGroupingServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private SemanticNewsEventRepository semanticNewsEventRepository;

    @Test
    void returnsRecentCandidatesFromConfiguredLookupWindow() {
        Category politics = category();
        SemanticNewsEvent event = semanticEvent(200L, "greek parliament debates migration bill", politics);
        when(semanticNewsEventRepository.findByLastSeenAtGreaterThanEqualOrderByLastSeenAtDesc(
                NOW.minus(Duration.ofDays(3))))
                .thenReturn(List.of(event));

        CandidateSemanticEvents candidates = service().recentCandidates();

        assertThat(candidates.lookupWindowStartedAt()).isEqualTo(NOW.minus(Duration.ofDays(3)));
        assertThat(candidates.lookupWindowEndedAt()).isEqualTo(NOW);
        assertThat(candidates.dtos()).singleElement().satisfies(candidate -> {
            assertThat(candidate.id()).isEqualTo("200");
            assertThat(candidate.semanticKey()).isEqualTo("greek parliament debates migration bill");
        });
        assertThat(candidates.eventsById()).containsEntry("200", event);
    }

    @Test
    void matchesExistingSemanticEventAndUpdatesLastSeenAt() {
        Category politics = category();
        SemanticNewsEvent event = semanticEvent(200L, "greek parliament debates migration bill", politics);
        CandidateSemanticEvents candidates = new CandidateSemanticEvents(
                List.of(),
                Map.of("200", event),
                NOW.minus(Duration.ofDays(3)),
                NOW);
        when(semanticNewsEventRepository.save(event)).thenReturn(event);

        var grouped = service().group(
                SemanticKeyActionDto.MATCHED_EXISTING,
                "greek parliament approves migration bill",
                "200",
                candidates,
                politics,
                NOW);

        assertThat(grouped.semanticEvent()).isSameAs(event);
        assertThat(grouped.matchedSemanticEvent()).isSameAs(event);
        assertThat(event.getLastSeenAt()).isEqualTo(NOW);
        verify(semanticNewsEventRepository).save(event);
    }

    @Test
    void createsNewSemanticEventWhenNoCandidateMatches() {
        Category politics = category();
        CandidateSemanticEvents candidates = new CandidateSemanticEvents(
                List.of(),
                Map.of(),
                NOW.minus(Duration.ofDays(3)),
                NOW);
        ArgumentCaptor<SemanticNewsEvent> eventCaptor = ArgumentCaptor.forClass(SemanticNewsEvent.class);
        when(semanticNewsEventRepository.save(eventCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var grouped = service().group(
                SemanticKeyActionDto.CREATED_NEW,
                "greek parliament approves migration bill",
                null,
                candidates,
                politics,
                NOW);

        assertThat(grouped.semanticEvent().getSemanticKey()).isEqualTo("greek parliament approves migration bill");
        assertThat(grouped.semanticEvent().getCategory()).isSameAs(politics);
        assertThat(grouped.semanticEvent().getFirstSeenAt()).isEqualTo(NOW);
        assertThat(grouped.matchedSemanticEvent()).isNull();
    }

    private SemanticEventGroupingService service() {
        return new SemanticEventGroupingService(
                semanticNewsEventRepository,
                openAiProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OpenAiProperties openAiProperties() {
        return new OpenAiProperties(
                "test-key",
                new OpenAiProperties.Models("gpt-5.4-mini", "gpt-5.5"),
                new OpenAiProperties.Prompts(
                        "classification-v1",
                        "editorial-rules-v1",
                        "publication-content-v1"),
                new OpenAiProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60)),
                Duration.ofDays(3));
    }

    private Category category() {
        Category category = Category.create("politics", "Politics", "Government", true, true, 10);
        ReflectionTestUtils.setField(category, "id", 1L);
        return category;
    }

    private SemanticNewsEvent semanticEvent(Long id, String semanticKey, Category category) {
        SemanticNewsEvent event = SemanticNewsEvent.create(semanticKey, category, NOW.minus(Duration.ofHours(1)));
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}

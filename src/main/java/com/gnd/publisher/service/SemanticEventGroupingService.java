package com.gnd.publisher.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gnd.publisher.config.ClassificationProperties;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.dto.llm.SemanticEventKeyCandidateDto;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;
import com.gnd.publisher.exception.LlmIntegrationException;
import com.gnd.publisher.repository.SemanticNewsEventRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SemanticEventGroupingService {

    private final SemanticNewsEventRepository semanticNewsEventRepository;
    private final ClassificationProperties classificationProperties;
    private final Clock clock;

    @Autowired
    public SemanticEventGroupingService(
            SemanticNewsEventRepository semanticNewsEventRepository,
            ClassificationProperties classificationProperties) {
        this(semanticNewsEventRepository, classificationProperties, Clock.systemUTC());
    }

    SemanticEventGroupingService(
            SemanticNewsEventRepository semanticNewsEventRepository,
            ClassificationProperties classificationProperties,
            Clock clock) {
        this.semanticNewsEventRepository = semanticNewsEventRepository;
        this.classificationProperties = classificationProperties;
        this.clock = clock;
    }

    public CandidateSemanticEvents recentCandidates() {
        Instant lookupWindowEndedAt = Instant.now(clock);
        Instant lookupWindowStartedAt = lookupWindowEndedAt.minus(classificationProperties.semanticEventLookupWindow());
        List<SemanticNewsEvent> events = semanticNewsEventRepository
                .findByLastSeenAtGreaterThanEqualOrderByLastSeenAtDesc(lookupWindowStartedAt);
        List<SemanticEventKeyCandidateDto> candidateDtos = events.stream()
                .map(event -> new SemanticEventKeyCandidateDto(String.valueOf(event.getId()), event.getSemanticKey()))
                .toList();
        Map<String, SemanticNewsEvent> eventsById = events.stream()
                .collect(Collectors.toUnmodifiableMap(event -> String.valueOf(event.getId()), Function.identity()));
        return new CandidateSemanticEvents(
                candidateDtos,
                eventsById,
                lookupWindowStartedAt,
                lookupWindowEndedAt);
    }

    public GroupedSemanticEvent group(
            SemanticKeyActionDto action,
            String returnedSemanticKey,
            String matchedSemanticEventId,
            CandidateSemanticEvents candidates,
            Category primaryCategory,
            Instant seenAt) {
        if (action == SemanticKeyActionDto.MATCHED_EXISTING) {
            SemanticNewsEvent event = Optional.ofNullable(candidates.eventsById().get(matchedSemanticEventId))
                    .orElseThrow(() -> new LlmIntegrationException(
                            "matchedSemanticEventId was not present in recent candidates"));
            event.markSeen(seenAt);
            event.setCategoryIfMissing(primaryCategory);
            return new GroupedSemanticEvent(semanticNewsEventRepository.save(event), event);
        }

        SemanticNewsEvent event = SemanticNewsEvent.create(returnedSemanticKey, primaryCategory, seenAt);
        return new GroupedSemanticEvent(semanticNewsEventRepository.save(event), null);
    }

    public record CandidateSemanticEvents(
            List<SemanticEventKeyCandidateDto> dtos,
            Map<String, SemanticNewsEvent> eventsById,
            Instant lookupWindowStartedAt,
            Instant lookupWindowEndedAt) {
    }

    public record GroupedSemanticEvent(
            SemanticNewsEvent semanticEvent,
            SemanticNewsEvent matchedSemanticEvent) {
    }
}

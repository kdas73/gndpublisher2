package com.gnd.publisher.repository;

import java.util.Optional;

import com.gnd.publisher.domain.model.Translation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface TranslationRepository extends JpaRepository<Translation, Long> {

    Optional<Translation> findBySemanticEvent_IdAndTargetLanguage(Long semanticEventId, String targetLanguage);

    @EntityGraph(attributePaths = {"semanticEvent", "semanticEvent.category", "newsItem", "newsItem.source"})
    Optional<Translation> findWithNewsItemById(Long id);

    void deleteByNewsItem_Id(Long newsItemId);
}

package com.gnd.publisher.repository;

import java.util.Optional;

import com.gnd.publisher.domain.model.Translation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationRepository extends JpaRepository<Translation, Long> {

    Optional<Translation> findBySemanticEvent_IdAndTargetLanguage(Long semanticEventId, String targetLanguage);
}

package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.Translation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationRepository extends JpaRepository<Translation, Long> {
}

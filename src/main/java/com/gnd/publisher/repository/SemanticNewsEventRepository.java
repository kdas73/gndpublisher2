package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.SemanticNewsEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SemanticNewsEventRepository extends JpaRepository<SemanticNewsEvent, Long> {
}

package com.gnd.publisher.repository;

import java.time.Instant;
import java.util.List;

import com.gnd.publisher.domain.model.SemanticNewsEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SemanticNewsEventRepository extends JpaRepository<SemanticNewsEvent, Long> {

    List<SemanticNewsEvent> findByLastSeenAtGreaterThanEqualOrderByLastSeenAtDesc(Instant lookupWindowStartedAt);
}

package com.gnd.publisher.repository;

import java.time.Instant;

import com.gnd.publisher.domain.model.ClassificationRun;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassificationRunRepository extends JpaRepository<ClassificationRun, Long> {

    void deleteByCreatedAtBefore(Instant cutoff);

    void deleteByNewsItem_Id(Long newsItemId);
}

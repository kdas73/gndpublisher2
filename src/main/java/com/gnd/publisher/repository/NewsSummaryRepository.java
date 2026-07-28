package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.NewsSummary;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSummaryRepository extends JpaRepository<NewsSummary, Long> {

    void deleteByNewsItem_Id(Long newsItemId);
}

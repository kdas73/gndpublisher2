package com.gnd.publisher.repository;

import java.util.List;

import com.gnd.publisher.domain.model.RssSource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RssSourceRepository extends JpaRepository<RssSource, Long> {

    List<RssSource> findByEnabledTrue();
}

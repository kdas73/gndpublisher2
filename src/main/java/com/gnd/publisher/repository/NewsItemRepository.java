package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.NewsItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {
}

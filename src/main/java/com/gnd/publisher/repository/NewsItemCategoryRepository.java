package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.NewsItemCategory;
import com.gnd.publisher.domain.model.NewsItemCategoryId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemCategoryRepository extends JpaRepository<NewsItemCategory, NewsItemCategoryId> {
}

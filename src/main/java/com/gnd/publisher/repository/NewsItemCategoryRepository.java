package com.gnd.publisher.repository;

import java.util.List;

import com.gnd.publisher.domain.model.NewsItemCategory;
import com.gnd.publisher.domain.model.NewsItemCategoryId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemCategoryRepository extends JpaRepository<NewsItemCategory, NewsItemCategoryId> {

    void deleteByNewsItem_Id(Long newsItemId);

    List<NewsItemCategory> findByNewsItem_Id(Long newsItemId);
}

package com.gnd.publisher.repository;

import java.util.List;

import com.gnd.publisher.domain.enums.ClassificationStatus;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    boolean existsBySourceAndExternalId(RssSource source, String externalId);

    boolean existsBySourceAndSourceUrl(RssSource source, String sourceUrl);

    List<NewsItem> findByClassificationStatus(ClassificationStatus classificationStatus);
}

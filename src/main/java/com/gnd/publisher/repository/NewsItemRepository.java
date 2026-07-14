package com.gnd.publisher.repository;

import java.util.List;

import com.gnd.publisher.domain.enums.ClassificationStatus;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    boolean existsBySourceAndExternalId(RssSource source, String externalId);

    boolean existsBySourceAndSourceUrl(RssSource source, String sourceUrl);

    List<NewsItem> findByClassificationStatus(ClassificationStatus classificationStatus);

    @EntityGraph(attributePaths = {"source", "semanticEvent", "semanticEvent.category"})
    @Query("select item from NewsItem item where item.id = :id")
    java.util.Optional<NewsItem> findPublicationContentContextById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"source", "semanticEvent", "semanticEvent.category"})
    List<NewsItem> findBySelectedForPublicationTrueAndPublicationProcessedAtIsNullAndSemanticEventIsNotNull();

    @Query("""
            select new com.gnd.publisher.repository.SourceQuotaCandidate(item, source.code, category.publicationPriority)
            from NewsItem item
            join item.source source
            join ClassificationRun run on run.newsItem = item
            join run.category category
            where item.publicationCandidate = true
              and item.classificationStatus = com.gnd.publisher.domain.enums.ClassificationStatus.CLASSIFIED
              and item.semanticEvent is not null
              and item.selectedForPublication = false
              and item.publicationProcessedAt is null
              and (
                  item.processingRunId = :processingRunId
                  or item.rejectionReason = com.gnd.publisher.domain.enums.RejectionReason.SOURCE_RUN_QUOTA_EXCEEDED
              )
              and run.id = (
                  select max(latestRun.id)
                  from ClassificationRun latestRun
                  where latestRun.newsItem = item
              )
            """)
    List<SourceQuotaCandidate> findSourceQuotaCandidates(@Param("processingRunId") String processingRunId);
}

package com.gnd.publisher.repository;

import java.time.Instant;
import java.util.List;

import com.gnd.publisher.domain.enums.PublicationStatus;
import com.gnd.publisher.domain.model.Publication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
            Long semanticEventId,
            Long telegramChannelId,
            String targetLanguage);

    List<Publication> findByStatusAndTargetLanguageAndPublishedAtGreaterThanEqual(
            PublicationStatus status,
            String targetLanguage,
            Instant publishedAtThreshold);

    @Query("""
            select publication from Publication publication
            where publication.createdAt < :cutoff
              and not exists (
                  select 1 from ImportantNewsDigestItem digestItem
                  where digestItem.publication = publication
              )
            """)
    List<Publication> findCleanupCandidates(@Param("cutoff") Instant cutoff);
}

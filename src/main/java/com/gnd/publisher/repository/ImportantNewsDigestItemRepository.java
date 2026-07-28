package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.ImportantNewsDigestItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportantNewsDigestItemRepository extends JpaRepository<ImportantNewsDigestItem, Long> {

    boolean existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
            Long semanticEventId,
            Long telegramChannelId,
            String targetLanguage);

    void deleteByImportantNewsDigestPost_Id(Long importantNewsDigestPostId);
}

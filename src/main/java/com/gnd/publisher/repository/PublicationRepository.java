package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.Publication;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsBySemanticEvent_IdAndTelegramChannel_IdAndTargetLanguage(
            Long semanticEventId,
            Long telegramChannelId,
            String targetLanguage);
}

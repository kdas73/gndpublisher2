package com.gnd.publisher.repository;

import java.util.List;
import java.util.Optional;

import com.gnd.publisher.domain.model.TelegramChannel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramChannelRepository extends JpaRepository<TelegramChannel, Long> {

    Optional<TelegramChannel> findByCode(String code);

    List<TelegramChannel> findByLanguageAndEnabledTrue(String language);
}

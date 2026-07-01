package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.TelegramChannel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramChannelRepository extends JpaRepository<TelegramChannel, Long> {
}

package com.gnd.publisher.service;

import java.util.List;

import com.gnd.publisher.config.TelegramProperties;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.repository.TelegramChannelRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramChannelSyncService {

    private final TelegramProperties telegramProperties;
    private final TelegramChannelRepository telegramChannelRepository;

    public TelegramChannelSyncService(
            TelegramProperties telegramProperties,
            TelegramChannelRepository telegramChannelRepository) {
        this.telegramProperties = telegramProperties;
        this.telegramChannelRepository = telegramChannelRepository;
    }

    @Transactional
    public List<TelegramChannel> syncConfiguredChannels() {
        return telegramProperties.channels().stream()
                .map(this::syncChannel)
                .toList();
    }

    private TelegramChannel syncChannel(TelegramProperties.Channel configuredChannel) {
        TelegramChannel channel = telegramChannelRepository.findByCode(configuredChannel.code())
                .orElseGet(() -> TelegramChannel.create(
                        configuredChannel.code(),
                        configuredChannel.language(),
                        configuredChannel.channelId(),
                        configuredChannel.username(),
                        configuredChannel.messageUrlTemplate(),
                        configuredChannel.code(),
                        configuredChannel.enabled()));
        channel.updateFromConfiguration(
                configuredChannel.language(),
                configuredChannel.channelId(),
                configuredChannel.username(),
                configuredChannel.messageUrlTemplate(),
                configuredChannel.code(),
                configuredChannel.enabled());
        return telegramChannelRepository.save(channel);
    }
}

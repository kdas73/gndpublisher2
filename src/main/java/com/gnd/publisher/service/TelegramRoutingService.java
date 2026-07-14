package com.gnd.publisher.service;

import java.util.List;

import com.gnd.publisher.config.ImportantNewsDigestProperties;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.repository.TelegramChannelRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramRoutingService {

    private final TelegramChannelRepository telegramChannelRepository;
    private final ImportantNewsDigestProperties digestProperties;

    public TelegramRoutingService(
            TelegramChannelRepository telegramChannelRepository,
            ImportantNewsDigestProperties digestProperties) {
        this.telegramChannelRepository = telegramChannelRepository;
        this.digestProperties = digestProperties;
    }

    @Transactional(readOnly = true)
    public List<TelegramChannel> publicationChannelsForLanguage(String targetLanguage) {
        return telegramChannelRepository.findByLanguageAndEnabledTrue(targetLanguage).stream()
                .filter(channel -> !channel.getCode().equals(digestProperties.channelCode()))
                .toList();
    }
}

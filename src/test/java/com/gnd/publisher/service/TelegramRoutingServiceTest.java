package com.gnd.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import com.gnd.publisher.config.ImportantNewsDigestProperties;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.repository.TelegramChannelRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramRoutingServiceTest {

    @Mock
    private TelegramChannelRepository telegramChannelRepository;

    @Test
    void returnsEnabledLanguageChannelsExcludingDigestChannel() {
        TelegramChannel news = channel("news-en", "en");
        TelegramChannel digest = channel("important-news-en", "en");
        when(telegramChannelRepository.findByLanguageAndEnabledTrue("en")).thenReturn(List.of(news, digest));

        List<TelegramChannel> channels = service().publicationChannelsForLanguage("en");

        assertThat(channels).containsExactly(news);
    }

    private TelegramRoutingService service() {
        return new TelegramRoutingService(
                telegramChannelRepository,
                new ImportantNewsDigestProperties(true, "0 0 */6 * * *", 2, "en", "important-news-en",
                        Duration.ofDays(7)));
    }

    private TelegramChannel channel(String code, String language) {
        return TelegramChannel.create(
                code,
                language,
                "-100123456",
                "gnd_news",
                "https://t.me/{username}/{messageId}",
                code,
                true);
    }
}

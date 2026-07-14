package com.gnd.publisher.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gnd.publisher.config.PublishingProperties;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;

import org.junit.jupiter.api.Test;

class TelegramMessageMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final String SOURCE_LINE =
            "\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a: <a href=\"https://example.test/news/item\">ERT News</a>";
    private static final String TOPIC_LINE = "\u0422\u0435\u043c\u0430: politics";

    @Test
    void mapsMessageWithSourceAttributionAndTopic() {
        TelegramMessageDto message = mapper(3500).toMessage(translation("Short summary"), channel());

        assertThat(message.chatId()).isEqualTo("-100123456");
        assertThat(message.parseMode()).isEqualTo("HTML");
        assertThat(message.text()).contains(
                "<a href=\"https://example.test/news/item\">Translated title</a>",
                "Short summary",
                SOURCE_LINE,
                TOPIC_LINE);
        assertThat(message.text()).contains(SOURCE_LINE + "\n" + TOPIC_LINE);
    }

    @Test
    void truncatesSummaryBeforeDroppingAttribution() {
        TelegramMessageDto message = mapper(220).toMessage(translation("Long summary ".repeat(20)), channel());

        assertThat(message.text()).hasSizeLessThanOrEqualTo(220);
        assertThat(message.text()).contains(SOURCE_LINE, TOPIC_LINE);
    }

    @Test
    void escapesHtmlInMessageText() {
        TelegramMessageDto message = mapper(3500).toMessage(translation("5 > 3 & 2 < 4"), channel());

        assertThat(message.text()).contains("5 &gt; 3 &amp; 2 &lt; 4");
    }

    private TelegramMessageMapper mapper(int maxMessageCharacters) {
        return new TelegramMessageMapper(new PublishingProperties(
                Set.of("ru"),
                3,
                Map.of(),
                600,
                maxMessageCharacters,
                new PublishingProperties.ImportantNewsDigest(true, 2, "important-news")));
    }

    private Translation translation(String summary) {
        RssSource source = RssSource.create(
                "ert-news",
                "ERT News",
                "https://feeds.example.test/ert",
                "el",
                true);
        NewsItem newsItem = NewsItem.fromRssFeedItem(
                source,
                new RssFeedItemDto(
                        Optional.of("external-1"),
                        "Source title",
                        Optional.of("https://example.test/news/item"),
                        Optional.of("Source summary"),
                        Optional.of(NOW)),
                NOW);
        Category category = Category.create("politics", "Politics", "Government", true, true, 10);
        SemanticNewsEvent semanticEvent = SemanticNewsEvent.create("source event", category, NOW);
        return Translation.publicationContent(
                semanticEvent,
                newsItem,
                "ru",
                "Translated title",
                summary,
                "openai",
                "gpt-5.5");
    }

    private TelegramChannel channel() {
        return TelegramChannel.create(
                "news-ru",
                "ru",
                "-100123456",
                "gnd_news",
                "https://t.me/{username}/{messageId}",
                "News RU",
                true);
    }
}

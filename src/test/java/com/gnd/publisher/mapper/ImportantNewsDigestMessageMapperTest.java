package com.gnd.publisher.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.ImportantNewsDigestItem;
import com.gnd.publisher.domain.model.ImportantNewsDigestPost;
import com.gnd.publisher.domain.model.NewsItem;
import com.gnd.publisher.domain.model.Publication;
import com.gnd.publisher.domain.model.RssSource;
import com.gnd.publisher.domain.model.SemanticNewsEvent;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ImportantNewsDigestMessageMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    private final ImportantNewsDigestMessageMapper mapper = new ImportantNewsDigestMessageMapper();

    @Test
    void mapsLinkedTitlesOnlyWithHeaderAndNoSummary() {
        ImportantNewsDigestItem first = digestItem("First important title", "https://t.me/gnd_news/10", 5);
        ImportantNewsDigestItem second = digestItem("Second important title", "https://t.me/gnd_news/20", 7);

        TelegramMessageDto message = mapper.toMessage(List.of(first, second), channel());

        assertThat(message.chatId()).isEqualTo("-100123456");
        assertThat(message.parseMode()).isEqualTo("HTML");
        assertThat(message.text()).contains(
                "<a href=\"https://t.me/gnd_news/10\">First important title</a>",
                "<a href=\"https://t.me/gnd_news/20\">Second important title</a>");
        assertThat(message.text()).doesNotContain("Source summary", "Translated summary");
    }

    @Test
    void escapesHtmlInTitles() {
        ImportantNewsDigestItem item = digestItem("5 > 3 & 2 < 4", "https://t.me/gnd_news/30", 3);

        TelegramMessageDto message = mapper.toMessage(List.of(item), channel());

        assertThat(message.text()).contains("5 &gt; 3 &amp; 2 &lt; 4");
    }

    @Test
    void preservesItemOrder() {
        ImportantNewsDigestItem first = digestItem("Alpha title", "https://t.me/gnd_news/1", 3);
        ImportantNewsDigestItem second = digestItem("Beta title", "https://t.me/gnd_news/2", 4);

        TelegramMessageDto message = mapper.toMessage(List.of(first, second), channel());

        int alphaIndex = message.text().indexOf("Alpha title");
        int betaIndex = message.text().indexOf("Beta title");
        assertThat(alphaIndex).isPositive();
        assertThat(betaIndex).isGreaterThan(alphaIndex);
    }

    private ImportantNewsDigestItem digestItem(String title, String publicationUrl, int sourceItemCount) {
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
        ReflectionTestUtils.setField(semanticEvent, "id", 200L);
        Translation translation = Translation.publicationContent(
                semanticEvent, newsItem, "ru", title, "Translated summary", "openai", "gpt-5.5");
        TelegramChannel telegramChannel = channel();
        Publication publication = Publication.pending(semanticEvent, newsItem, translation, telegramChannel, "ru");
        publication.markPublished("10", publicationUrl, NOW);
        ImportantNewsDigestPost post = ImportantNewsDigestPost.pending(telegramChannel, "ru", 2);
        return ImportantNewsDigestItem.create(
                post, semanticEvent, publication, telegramChannel, "ru", title, publicationUrl, sourceItemCount, NOW);
    }

    private TelegramChannel channel() {
        return TelegramChannel.create(
                "important-news-ru",
                "ru",
                "-100123456",
                "gnd_important_news",
                "https://t.me/{username}/{messageId}",
                "Important News RU",
                true);
    }
}

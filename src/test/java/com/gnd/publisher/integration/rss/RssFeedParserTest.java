package com.gnd.publisher.integration.rss;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.gnd.publisher.dto.rss.RssFeedItemDto;

import org.junit.jupiter.api.Test;

class RssFeedParserTest {

    private final RssFeedParser parser = new RssFeedParser();

    @Test
    void parsesRssItemsFromFixture() throws Exception {
        List<RssFeedItemDto> items = parser.parse(fixture("sample-feed.xml"));

        assertThat(items).hasSize(2);
        assertThat(items.getFirst().externalId()).contains("article-1");
        assertThat(items.getFirst().title()).isEqualTo("First fixture article");
        assertThat(items.getFirst().link()).contains("https://example.test/news/first");
        assertThat(items.getFirst().summary()).contains("Fixture summary for the first article.");
        assertThat(items.getFirst().publishedAt()).contains(Instant.parse("2026-07-01T10:15:30Z"));
    }

    @Test
    void leavesMissingOptionalFieldsEmpty() throws Exception {
        List<RssFeedItemDto> items = parser.parse(fixture("sample-feed.xml"));

        assertThat(items.get(1).externalId()).isEmpty();
        assertThat(items.get(1).summary()).isEmpty();
        assertThat(items.get(1).publishedAt()).isEmpty();
    }

    private String fixture(String fileName) throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/fixtures/rss/" + fileName)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

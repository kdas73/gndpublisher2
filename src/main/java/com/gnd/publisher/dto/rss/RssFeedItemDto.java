package com.gnd.publisher.dto.rss;

import java.time.Instant;
import java.util.Optional;

public record RssFeedItemDto(
        Optional<String> externalId,
        String title,
        Optional<String> link,
        Optional<String> summary,
        Optional<Instant> publishedAt) {
}

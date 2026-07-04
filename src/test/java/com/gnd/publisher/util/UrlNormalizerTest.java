package com.gnd.publisher.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private final UrlNormalizer urlNormalizer = new UrlNormalizer();

    @Test
    void lowercasesSchemeAndHostAndRemovesDefaultPort() {
        String normalized = urlNormalizer.normalize(" HTTPS://Example.TEST:443/news/story ");

        assertThat(normalized).isEqualTo("https://example.test/news/story");
    }

    @Test
    void removesFragmentsAndTrackingParameters() {
        String normalized = urlNormalizer.normalize(
                "https://example.test/news/story?utm_source=rss&id=42&fbclid=abc#comments");

        assertThat(normalized).isEqualTo("https://example.test/news/story?id=42");
    }

    @Test
    void sortsRemainingQueryParameters() {
        String normalized = urlNormalizer.normalize("https://example.test/news/story?b=2&a=1");

        assertThat(normalized).isEqualTo("https://example.test/news/story?a=1&b=2");
    }

    @Test
    void normalizesPathDotSegments() {
        String normalized = urlNormalizer.normalize("https://example.test/news/../story");

        assertThat(normalized).isEqualTo("https://example.test/story");
    }

    @Test
    void returnsTrimmedFallbackForMalformedUrls() {
        String normalized = urlNormalizer.normalize(" https://example.test/a path ");

        assertThat(normalized).isEqualTo("https://example.test/a path");
    }

    @Test
    void returnsEmptyStringForNullOrBlankUrls() {
        assertThat(urlNormalizer.normalize(null)).isEmpty();
        assertThat(urlNormalizer.normalize("  ")).isEmpty();
    }
}

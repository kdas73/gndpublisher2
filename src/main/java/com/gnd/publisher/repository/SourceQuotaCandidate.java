package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.NewsItem;

public record SourceQuotaCandidate(NewsItem newsItem, String sourceCode, int publicationPriority) {
}

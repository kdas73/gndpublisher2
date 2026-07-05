package com.gnd.publisher.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "news_item_categories")
public class NewsItemCategory {

    @EmbeddedId
    private NewsItemCategoryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("newsItemId")
    @JoinColumn(name = "news_item_id", nullable = false)
    private NewsItem newsItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "matched_by", nullable = false, length = 100)
    private String matchedBy;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NewsItemCategory() {
    }

    private NewsItemCategory(
            NewsItem newsItem,
            Category category,
            String matchedBy,
            String model,
            BigDecimal confidence,
            Instant createdAt) {
        this.id = new NewsItemCategoryId(newsItem.getId(), category.getId());
        this.newsItem = newsItem;
        this.category = category;
        this.matchedBy = matchedBy;
        this.model = model;
        this.confidence = confidence;
        this.createdAt = createdAt;
    }

    public static NewsItemCategory classifierMatch(
            NewsItem newsItem,
            Category category,
            String model,
            BigDecimal confidence,
            Instant createdAt) {
        return new NewsItemCategory(newsItem, category, "OPENAI", model, confidence, createdAt);
    }

    public NewsItemCategoryId getId() {
        return id;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public Category getCategory() {
        return category;
    }

    public String getMatchedBy() {
        return matchedBy;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

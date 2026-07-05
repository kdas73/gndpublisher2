package com.gnd.publisher.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "translations")
public class Translation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semantic_event_id", nullable = false)
    private SemanticNewsEvent semanticEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_item_id", nullable = false)
    private NewsItem newsItem;

    @Column(name = "target_language", nullable = false, length = 16)
    private String targetLanguage;

    @Column(nullable = false, length = 1000)
    private String title;

    @Lob
    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    protected Translation() {
    }

    private Translation(
            SemanticNewsEvent semanticEvent,
            NewsItem newsItem,
            String targetLanguage,
            String title,
            String summary,
            String provider,
            String model) {
        this.semanticEvent = semanticEvent;
        this.newsItem = newsItem;
        this.targetLanguage = targetLanguage;
        this.title = title;
        this.summary = summary;
        this.provider = provider;
        this.model = model;
    }

    public static Translation publicationContent(
            SemanticNewsEvent semanticEvent,
            NewsItem newsItem,
            String targetLanguage,
            String title,
            String summary,
            String provider,
            String model) {
        return new Translation(semanticEvent, newsItem, targetLanguage, title, summary, provider, model);
    }

    public Long getId() {
        return id;
    }

    public SemanticNewsEvent getSemanticEvent() {
        return semanticEvent;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }
}

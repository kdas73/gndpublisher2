package com.gnd.publisher.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.gnd.publisher.domain.enums.SemanticKeyAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "classification_runs")
public class ClassificationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_item_id", nullable = false)
    private NewsItem newsItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semantic_event_id")
    private SemanticNewsEvent semanticEvent;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "input_keys_count", nullable = false)
    private int inputKeysCount;

    @Column(name = "lookup_window_started_at")
    private Instant lookupWindowStartedAt;

    @Column(name = "lookup_window_ended_at")
    private Instant lookupWindowEndedAt;

    @Column(name = "editorial_rules_version", length = 100)
    private String editorialRulesVersion;

    @Column(name = "returned_semantic_key", length = 500)
    private String returnedSemanticKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "semantic_key_action", length = 50)
    private SemanticKeyAction semanticKeyAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_semantic_event_id")
    private SemanticNewsEvent matchedSemanticEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Lob
    @Column(name = "raw_response")
    private String rawResponse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClassificationRun() {
    }

    private ClassificationRun(
            NewsItem newsItem,
            SemanticNewsEvent semanticEvent,
            String model,
            int inputKeysCount,
            Instant lookupWindowStartedAt,
            Instant lookupWindowEndedAt,
            String editorialRulesVersion,
            String returnedSemanticKey,
            SemanticKeyAction semanticKeyAction,
            SemanticNewsEvent matchedSemanticEvent,
            Category category,
            BigDecimal confidence,
            String rawResponse,
            Instant createdAt) {
        this.newsItem = newsItem;
        this.semanticEvent = semanticEvent;
        this.model = model;
        this.inputKeysCount = inputKeysCount;
        this.lookupWindowStartedAt = lookupWindowStartedAt;
        this.lookupWindowEndedAt = lookupWindowEndedAt;
        this.editorialRulesVersion = editorialRulesVersion;
        this.returnedSemanticKey = returnedSemanticKey;
        this.semanticKeyAction = semanticKeyAction;
        this.matchedSemanticEvent = matchedSemanticEvent;
        this.category = category;
        this.confidence = confidence;
        this.rawResponse = rawResponse;
        this.createdAt = createdAt;
    }

    public static ClassificationRun recordDecision(
            NewsItem newsItem,
            SemanticNewsEvent semanticEvent,
            String model,
            int inputKeysCount,
            Instant lookupWindowStartedAt,
            Instant lookupWindowEndedAt,
            String editorialRulesVersion,
            String returnedSemanticKey,
            SemanticKeyAction semanticKeyAction,
            SemanticNewsEvent matchedSemanticEvent,
            Category category,
            BigDecimal confidence,
            String rawResponse,
            Instant createdAt) {
        return new ClassificationRun(
                newsItem,
                semanticEvent,
                model,
                inputKeysCount,
                lookupWindowStartedAt,
                lookupWindowEndedAt,
                editorialRulesVersion,
                returnedSemanticKey,
                semanticKeyAction,
                matchedSemanticEvent,
                category,
                confidence,
                rawResponse,
                createdAt);
    }

    public Long getId() {
        return id;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public SemanticNewsEvent getSemanticEvent() {
        return semanticEvent;
    }

    public String getModel() {
        return model;
    }

    public int getInputKeysCount() {
        return inputKeysCount;
    }

    public Instant getLookupWindowStartedAt() {
        return lookupWindowStartedAt;
    }

    public Instant getLookupWindowEndedAt() {
        return lookupWindowEndedAt;
    }

    public String getEditorialRulesVersion() {
        return editorialRulesVersion;
    }

    public String getReturnedSemanticKey() {
        return returnedSemanticKey;
    }

    public SemanticKeyAction getSemanticKeyAction() {
        return semanticKeyAction;
    }

    public SemanticNewsEvent getMatchedSemanticEvent() {
        return matchedSemanticEvent;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

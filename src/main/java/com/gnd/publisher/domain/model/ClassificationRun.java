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

    public Long getId() {
        return id;
    }
}

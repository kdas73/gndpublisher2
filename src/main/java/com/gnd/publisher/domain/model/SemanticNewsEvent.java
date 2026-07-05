package com.gnd.publisher.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "semantic_news_events")
public class SemanticNewsEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semantic_key", nullable = false, length = 500)
    private String semanticKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false, length = 50)
    private String status;

    protected SemanticNewsEvent() {
    }

    private SemanticNewsEvent(String semanticKey, Category category, Instant seenAt) {
        this.semanticKey = semanticKey;
        this.category = category;
        this.firstSeenAt = seenAt;
        this.lastSeenAt = seenAt;
        this.status = "ACTIVE";
    }

    public static SemanticNewsEvent create(String semanticKey, Category category, Instant seenAt) {
        return new SemanticNewsEvent(semanticKey, category, seenAt);
    }

    public void markSeen(Instant seenAt) {
        this.lastSeenAt = seenAt;
    }

    public void setCategoryIfMissing(Category category) {
        if (this.category == null) {
            this.category = category;
        }
    }

    public Long getId() {
        return id;
    }

    public String getSemanticKey() {
        return semanticKey;
    }

    public Category getCategory() {
        return category;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public String getStatus() {
        return status;
    }
}

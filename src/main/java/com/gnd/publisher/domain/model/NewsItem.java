package com.gnd.publisher.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.gnd.publisher.domain.enums.ClassificationStatus;
import com.gnd.publisher.domain.enums.RejectionReason;

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
@Table(name = "news_items")
public class NewsItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private RssSource source;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "external_id", length = 500)
    private String externalId;

    @Column(nullable = false, length = 1000)
    private String title;

    @Lob
    private String summary;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "original_language", nullable = false, length = 16)
    private String originalLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semantic_event_id")
    private SemanticNewsEvent semanticEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 50)
    private ClassificationStatus classificationStatus;

    @Column(name = "classification_confidence", precision = 5, scale = 4)
    private BigDecimal classificationConfidence;

    @Column(name = "classified_at")
    private Instant classifiedAt;

    @Column(name = "processing_run_id", length = 100)
    private String processingRunId;

    @Column(name = "publication_candidate", nullable = false)
    private boolean publicationCandidate;

    @Column(name = "selected_for_publication", nullable = false)
    private boolean selectedForPublication;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 100)
    private RejectionReason rejectionReason;

    protected NewsItem() {
    }

    public Long getId() {
        return id;
    }
}

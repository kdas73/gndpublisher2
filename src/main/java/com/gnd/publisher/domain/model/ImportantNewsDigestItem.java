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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "important_news_digest_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_digest_items_event_channel_language",
                columnNames = {"semantic_event_id", "telegram_channel_id", "target_language"}))
public class ImportantNewsDigestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "important_news_digest_post_id", nullable = false)
    private ImportantNewsDigestPost importantNewsDigestPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semantic_event_id", nullable = false)
    private SemanticNewsEvent semanticEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telegram_channel_id", nullable = false)
    private TelegramChannel telegramChannel;

    @Column(name = "target_language", nullable = false, length = 16)
    private String targetLanguage;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(name = "publication_url", nullable = false, length = 1000)
    private String publicationUrl;

    @Column(name = "source_item_count", nullable = false)
    private int sourceItemCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImportantNewsDigestItem() {
    }

    public Long getId() {
        return id;
    }
}

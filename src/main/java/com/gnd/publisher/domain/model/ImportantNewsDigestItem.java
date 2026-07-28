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

    private ImportantNewsDigestItem(
            ImportantNewsDigestPost importantNewsDigestPost,
            SemanticNewsEvent semanticEvent,
            Publication publication,
            TelegramChannel telegramChannel,
            String targetLanguage,
            String title,
            String publicationUrl,
            int sourceItemCount,
            Instant createdAt) {
        this.importantNewsDigestPost = importantNewsDigestPost;
        this.semanticEvent = semanticEvent;
        this.publication = publication;
        this.telegramChannel = telegramChannel;
        this.targetLanguage = targetLanguage;
        this.title = title;
        this.publicationUrl = publicationUrl;
        this.sourceItemCount = sourceItemCount;
        this.createdAt = createdAt;
    }

    public static ImportantNewsDigestItem create(
            ImportantNewsDigestPost importantNewsDigestPost,
            SemanticNewsEvent semanticEvent,
            Publication publication,
            TelegramChannel telegramChannel,
            String targetLanguage,
            String title,
            String publicationUrl,
            int sourceItemCount,
            Instant createdAt) {
        return new ImportantNewsDigestItem(
                importantNewsDigestPost,
                semanticEvent,
                publication,
                telegramChannel,
                targetLanguage,
                title,
                publicationUrl,
                sourceItemCount,
                createdAt);
    }

    public Long getId() {
        return id;
    }

    public ImportantNewsDigestPost getImportantNewsDigestPost() {
        return importantNewsDigestPost;
    }

    public SemanticNewsEvent getSemanticEvent() {
        return semanticEvent;
    }

    public Publication getPublication() {
        return publication;
    }

    public TelegramChannel getTelegramChannel() {
        return telegramChannel;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getTitle() {
        return title;
    }

    public String getPublicationUrl() {
        return publicationUrl;
    }

    public int getSourceItemCount() {
        return sourceItemCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

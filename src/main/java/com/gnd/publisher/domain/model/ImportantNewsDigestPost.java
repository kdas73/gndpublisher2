package com.gnd.publisher.domain.model;

import java.time.Instant;

import com.gnd.publisher.domain.enums.PublicationStatus;

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
@Table(name = "important_news_digest_posts")
public class ImportantNewsDigestPost extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telegram_channel_id", nullable = false)
    private TelegramChannel telegramChannel;

    @Column(name = "target_language", nullable = false, length = 16)
    private String targetLanguage;

    @Column(name = "duplicate_threshold", nullable = false)
    private int duplicateThreshold;

    @Column(name = "telegram_message_id", length = 100)
    private String telegramMessageId;

    @Column(name = "telegram_message_url", length = 1000)
    private String telegramMessageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PublicationStatus status;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected ImportantNewsDigestPost() {
    }

    private ImportantNewsDigestPost(
            TelegramChannel telegramChannel,
            String targetLanguage,
            int duplicateThreshold) {
        this.telegramChannel = telegramChannel;
        this.targetLanguage = targetLanguage;
        this.duplicateThreshold = duplicateThreshold;
        this.status = PublicationStatus.PENDING;
    }

    public static ImportantNewsDigestPost pending(
            TelegramChannel telegramChannel,
            String targetLanguage,
            int duplicateThreshold) {
        return new ImportantNewsDigestPost(telegramChannel, targetLanguage, duplicateThreshold);
    }

    public void markPublished(String telegramMessageId, String telegramMessageUrl, Instant publishedAt) {
        this.telegramMessageId = telegramMessageId;
        this.telegramMessageUrl = telegramMessageUrl;
        this.publishedAt = publishedAt;
        this.errorMessage = null;
        this.status = PublicationStatus.PUBLISHED;
    }

    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = PublicationStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public TelegramChannel getTelegramChannel() {
        return telegramChannel;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public int getDuplicateThreshold() {
        return duplicateThreshold;
    }

    public String getTelegramMessageId() {
        return telegramMessageId;
    }

    public String getTelegramMessageUrl() {
        return telegramMessageUrl;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}

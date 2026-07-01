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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "publications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_publications_event_channel_language",
                columnNames = {"semantic_event_id", "telegram_channel_id", "target_language"}))
public class Publication extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semantic_event_id", nullable = false)
    private SemanticNewsEvent semanticEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_item_id", nullable = false)
    private NewsItem newsItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id")
    private Translation translation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telegram_channel_id", nullable = false)
    private TelegramChannel telegramChannel;

    @Column(name = "target_language", nullable = false, length = 16)
    private String targetLanguage;

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

    protected Publication() {
    }

    public Long getId() {
        return id;
    }
}

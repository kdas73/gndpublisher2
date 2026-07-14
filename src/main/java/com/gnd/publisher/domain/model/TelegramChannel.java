package com.gnd.publisher.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "telegram_channels")
public class TelegramChannel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 16)
    private String language;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    private String username;

    @Column(name = "message_url_template", nullable = false, length = 1000)
    private String messageUrlTemplate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    protected TelegramChannel() {
    }

    private TelegramChannel(
            String code,
            String language,
            String channelId,
            String username,
            String messageUrlTemplate,
            String name,
            boolean enabled) {
        this.code = code;
        this.language = language;
        this.channelId = channelId;
        this.username = username;
        this.messageUrlTemplate = messageUrlTemplate;
        this.name = name;
        this.enabled = enabled;
    }

    public static TelegramChannel create(
            String code,
            String language,
            String channelId,
            String username,
            String messageUrlTemplate,
            String name,
            boolean enabled) {
        return new TelegramChannel(code, language, channelId, username, messageUrlTemplate, name, enabled);
    }

    public void updateFromConfiguration(
            String language,
            String channelId,
            String username,
            String messageUrlTemplate,
            String name,
            boolean enabled) {
        this.language = language;
        this.channelId = channelId;
        this.username = username;
        this.messageUrlTemplate = messageUrlTemplate;
        this.name = name;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLanguage() {
        return language;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getUsername() {
        return username;
    }

    public String getMessageUrlTemplate() {
        return messageUrlTemplate;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

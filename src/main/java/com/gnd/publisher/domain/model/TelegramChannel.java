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

    public Long getId() {
        return id;
    }
}

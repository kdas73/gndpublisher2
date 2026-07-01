package com.gnd.publisher.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rss_sources")
public class RssSource extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    @Column(nullable = false, length = 16)
    private String language;

    @Column(nullable = false)
    private boolean enabled;

    protected RssSource() {
    }

    private RssSource(String name, String url, String language, boolean enabled) {
        this.name = name;
        this.url = url;
        this.language = language;
        this.enabled = enabled;
    }

    public static RssSource create(String name, String url, String language, boolean enabled) {
        return new RssSource(name, url, language, enabled);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

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

    public Long getId() {
        return id;
    }
}

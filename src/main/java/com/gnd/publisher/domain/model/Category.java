package com.gnd.publisher.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean publishable;

    @Column(name = "publication_priority", nullable = false)
    private int publicationPriority;

    protected Category() {
    }

    private Category(
            String code,
            String name,
            String description,
            boolean enabled,
            boolean publishable,
            int publicationPriority) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.publishable = publishable;
        this.publicationPriority = publicationPriority;
    }

    public static Category create(
            String code,
            String name,
            String description,
            boolean enabled,
            boolean publishable,
            int publicationPriority) {
        return new Category(code, name, description, enabled, publishable, publicationPriority);
    }

    public void updateFromConfiguration(
            String name,
            String description,
            boolean enabled,
            boolean publishable,
            int publicationPriority) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.publishable = publishable;
        this.publicationPriority = publicationPriority;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPublishable() {
        return publishable;
    }

    public int getPublicationPriority() {
        return publicationPriority;
    }
}

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

    public Long getId() {
        return id;
    }
}

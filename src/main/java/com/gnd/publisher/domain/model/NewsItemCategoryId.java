package com.gnd.publisher.domain.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class NewsItemCategoryId implements Serializable {

    @Column(name = "news_item_id")
    private Long newsItemId;

    @Column(name = "category_id")
    private Long categoryId;

    protected NewsItemCategoryId() {
    }

    public NewsItemCategoryId(Long newsItemId, Long categoryId) {
        this.newsItemId = newsItemId;
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NewsItemCategoryId that)) {
            return false;
        }
        return Objects.equals(newsItemId, that.newsItemId) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newsItemId, categoryId);
    }
}

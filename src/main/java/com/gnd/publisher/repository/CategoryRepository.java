package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

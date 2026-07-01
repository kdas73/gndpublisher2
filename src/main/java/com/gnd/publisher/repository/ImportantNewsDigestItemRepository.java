package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.ImportantNewsDigestItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportantNewsDigestItemRepository extends JpaRepository<ImportantNewsDigestItem, Long> {
}

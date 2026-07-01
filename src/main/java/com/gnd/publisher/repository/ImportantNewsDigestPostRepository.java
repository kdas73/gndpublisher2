package com.gnd.publisher.repository;

import com.gnd.publisher.domain.model.ImportantNewsDigestPost;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportantNewsDigestPostRepository extends JpaRepository<ImportantNewsDigestPost, Long> {
}

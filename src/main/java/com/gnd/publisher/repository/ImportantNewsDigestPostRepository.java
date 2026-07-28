package com.gnd.publisher.repository;

import java.time.Instant;
import java.util.List;

import com.gnd.publisher.domain.model.ImportantNewsDigestPost;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportantNewsDigestPostRepository extends JpaRepository<ImportantNewsDigestPost, Long> {

    List<ImportantNewsDigestPost> findByCreatedAtBefore(Instant cutoff);
}

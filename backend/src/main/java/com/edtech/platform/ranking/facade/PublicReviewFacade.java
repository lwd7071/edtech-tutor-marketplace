package com.edtech.platform.ranking.facade;

import com.edtech.platform.ranking.facade.dto.PublicReviewSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PublicReviewFacade {
    Page<PublicReviewSnapshot> getPublicReviews(UUID teacherId, Pageable pageable);
}

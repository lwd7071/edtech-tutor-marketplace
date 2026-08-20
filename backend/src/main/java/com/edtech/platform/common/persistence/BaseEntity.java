package com.edtech.platform.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class BaseEntity {
    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}

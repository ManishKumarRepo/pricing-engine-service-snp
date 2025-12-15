package com.pricing.pricingengine.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "price_batch")
public class BatchEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected BatchEntity() {}

    public BatchEntity(String id) {
        this.id = id;
        this.status = BatchStatus.STARTED;
        this.createdAt = Instant.now();
    }

    public void complete() {
        if (status != BatchStatus.STARTED) {
            throw new IllegalStateException("Invalid batch transition");
        }
        status = BatchStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void cancel() {
        if (status == BatchStatus.COMPLETED) {
            throw new IllegalStateException("Completed batch cannot be cancelled");
        }
        status = BatchStatus.CANCELLED;
    }
}

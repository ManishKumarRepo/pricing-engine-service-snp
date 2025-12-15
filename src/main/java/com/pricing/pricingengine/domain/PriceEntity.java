package com.pricing.pricingengine.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(
        name = "price_record",
        indexes = {
                @Index(columnList = "instrumentId, asOf")
        }
)
public class PriceEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String instrumentId;

    @Column(nullable = false)
    private Instant asOf;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private BatchEntity batch;

    protected PriceEntity() {}

    public PriceEntity(String instrumentId, Instant asOf, String payloadJson, BatchEntity batch) {
        this.instrumentId = instrumentId;
        this.asOf = asOf;
        this.payloadJson = payloadJson;
        this.batch = batch;
    }
}

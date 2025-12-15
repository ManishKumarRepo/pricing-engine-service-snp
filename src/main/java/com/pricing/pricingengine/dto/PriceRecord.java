package com.pricing.pricingengine.dto;

import java.time.Instant;

public record PriceRecord(
        String instrumentId,
        Instant asOf,
        String payloadJson
) {}


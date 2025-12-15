package com.pricing.pricingengine.dto;

import java.time.Instant;

public record LastPriceResponse(
        String instrumentId,
        Instant asOf,
        String payloadJson
) {}


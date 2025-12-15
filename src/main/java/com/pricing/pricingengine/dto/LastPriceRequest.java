package com.pricing.pricingengine.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record LastPriceRequest(
        @NotEmpty Set<String> instrumentIds
) {}


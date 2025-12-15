package com.pricing.pricingengine.controller;

import com.pricing.pricingengine.domain.PriceEntity;
import com.pricing.pricingengine.dto.LastPriceRequest;
import com.pricing.pricingengine.dto.LastPriceResponse;
import com.pricing.pricingengine.service.PriceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prices")
@Tag(name = "B) CONSUME LAST PRICES API", description = "ENDPOINT FOR CONSUMING LAST UPLOADED PRICES FOR INSTRUMENT.")
public class PriceConsumeController {

    private static final Logger log = LoggerFactory.getLogger(PriceConsumeController.class);

    private final PriceQueryService queryService;

    public PriceConsumeController(PriceQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Fetches the last price per instrument.
     * Only prices from COMPLETED batches are visible.
     */
    @Operation(
            summary = "FETCHES THE LAST PRICE PER INSTRUMENT"
    )
    @PostMapping("/last")
    public ResponseEntity<List<LastPriceResponse>> getLastPrices(
            @RequestBody @Valid LastPriceRequest request) {

        log.debug("Fetching last prices for instruments {}", request.instrumentIds());

        if (request.instrumentIds() == null || request.instrumentIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<PriceEntity> prices =
                queryService.getLastPrices(request.instrumentIds());

        List<LastPriceResponse> response = prices.stream()
                .map(p -> new LastPriceResponse(
                        p.getInstrumentId(),
                        p.getAsOf(),
                        p.getPayloadJson()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}


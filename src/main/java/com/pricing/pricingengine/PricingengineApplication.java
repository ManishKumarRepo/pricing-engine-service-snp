package com.pricing.pricingengine;

import com.pricing.pricingengine.dto.PriceRecord;
import com.pricing.pricingengine.service.BatchService;
import com.pricing.pricingengine.service.PriceQueryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class PricingengineApplication implements CommandLineRunner {

    private final BatchService batchService;
    private final PriceQueryService queryService;

    public PricingengineApplication(BatchService batchService,
                              PriceQueryService queryService) {
        this.batchService = batchService;
        this.queryService = queryService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PricingengineApplication.class, args);
    }

    @Override
    public void run(String... args) {

        String batchId = "batch-initial-upload";

        batchService.startBatch(batchId);

        batchService.uploadPrices(batchId, List.of(
                new PriceRecord("META", Instant.parse("2024-01-01T10:00:00Z"), "{\"price\":180}"),
                new PriceRecord("META", Instant.parse("2024-01-01T11:00:00Z"), "{\"price\":182}"),
                new PriceRecord("MSFT", Instant.parse("2024-01-01T09:00:00Z"), "{\"price\":310}")
        ));

        batchService.completeBatch(batchId);

        queryService.getLastPrices(Set.of("META", "MSFT"))
                .forEach(p ->
                        System.out.println(p.getInstrumentId() + " => " + p.getPayloadJson())
                );
    }
}


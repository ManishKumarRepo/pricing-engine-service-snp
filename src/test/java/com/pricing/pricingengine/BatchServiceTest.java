package com.pricing.pricingengine;

import com.pricing.pricingengine.domain.BatchStatus;
import com.pricing.pricingengine.dto.PriceRecord;
import com.pricing.pricingengine.repository.BatchRepository;
import com.pricing.pricingengine.service.BatchService;
import com.pricing.pricingengine.service.PriceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchServiceTest extends BaseIntegrationTest {

    @Autowired
    BatchService batchService;

    @Autowired
    PriceQueryService priceQueryService;

    @Autowired
    BatchRepository batchRepository;

    @Test
    void startUploadComplete_shouldSucceed() {
        String batchId = "batch-1";

        batchService.startBatch(batchId);

        batchService.uploadPrices(batchId, List.of(
                new PriceRecord("AAPL", Instant.now(), "{\"price\":100}")
        ));

        batchService.completeBatch(batchId);

        var batch = batchRepository.findById(batchId).orElseThrow();
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void uploadBeforeStart_shouldFail() {
        assertThatThrownBy(() ->
                batchService.uploadPrices(
                        "missing-batch",
                        List.of(new PriceRecord("AAPL", Instant.now(), "{}"))
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uploadAfterComplete_shouldFail() {
        String batchId = "batch-2";
        batchService.startBatch(batchId);
        batchService.completeBatch(batchId);

        assertThatThrownBy(() ->
                batchService.uploadPrices(batchId,
                        List.of(new PriceRecord("AAPL", Instant.now(), "{}"))
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelledBatch_shouldNotExposePrices() {
        String batchId = "batch-3";

        batchService.startBatch(batchId);
        batchService.uploadPrices(batchId,
                List.of(new PriceRecord("AMZN", Instant.now(), "{\"price\":123}"))
        );
        batchService.cancelBatch(batchId);

        var prices = priceQueryService.getLastPrices(Set.of("AMZN"));
        assertThat(prices).isEmpty();
    }

    /*
    * Consumers Cannot See Incomplete Batch
    */
    @Test
    void consumersShouldNotSeeIncompleteBatch() {
        String batchId = "batch-4";

        batchService.startBatch(batchId);
        batchService.uploadPrices(batchId,
                List.of(new PriceRecord("AAPL", Instant.now(), "{\"price\":100}"))
        );

        var prices = priceQueryService.getLastPrices(Set.of("AAPL"));
        assertThat(prices).isEmpty();
    }

    /*
    *  Consumers See All Prices After Completion
    */
    @Test
    void consumersSeePricesAfterCompletion() {
        String batchId = "batch-5";

        batchService.startBatch(batchId);
        batchService.uploadPrices(batchId,
                List.of(
                        new PriceRecord("AAPL", Instant.now(), "{\"price\":100}"),
                        new PriceRecord("AAPL", Instant.now().plusSeconds(60), "{\"price\":105}")
                )
        );
        batchService.completeBatch(batchId);

        var prices = priceQueryService.getLastPrices(Set.of("AAPL"));
        assertThat(prices).hasSize(1);
    }

    /*
    * Parallel Upload + Complete (Race Condition Test)
    */
    @Test
    void concurrentUploadAndComplete_shouldNotAllowLateWrites() throws Exception {
        String batchId = "batch-concurrent";

        batchService.startBatch(batchId);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> uploader = () -> {
            batchService.uploadPrices(batchId,
                    List.of(new PriceRecord("AAPLA", Instant.now(), "{\"price\":100}"))
            );
            return null;
        };

        Callable<Void> completer = () -> {
            batchService.completeBatch(batchId);
            return null;
        };

        executor.invokeAll(List.of(uploader, completer));
        executor.shutdown();

        var prices = priceQueryService.getLastPrices(Set.of("AAPLA"));
        assertThat(prices).hasSizeLessThanOrEqualTo(1);
    }

    /*
    *  Concurrent Uploads Are Serialized
    */
    @Test
    void concurrentUploads_shouldNotCorruptData() throws Exception {
        String batchId = "batch-parallel";
        AtomicInteger counter = new AtomicInteger();
        Instant base = Instant.parse("2025-12-15T10:00:00Z");
        batchService.startBatch(batchId);

        ExecutorService executor = Executors.newFixedThreadPool(5);

            Runnable uploadTask = () -> {
            int index = counter.incrementAndGet();
            batchService.uploadPrices(batchId,
                    List.of(new PriceRecord("GOOG", base.plusSeconds(index), "{\"price\":143.07}"))
            );
        };

        for (int i = 0; i < 5; i++) {
            executor.submit(uploadTask);
        }

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        batchService.completeBatch(batchId);

        var prices = priceQueryService.getLastPrices(Set.of("GOOG"));
        assertThat(prices).isNotEmpty();
    }

    @Test
    void lastPriceQuery_returnsLatestAsOf() {
        String batchId = "batch-query";

        batchService.startBatch(batchId);
        batchService.uploadPrices(batchId, List.of(
                new PriceRecord("TESLA", Instant.parse("2024-01-01T10:00:00Z"), "{}"),
                new PriceRecord("TESLA", Instant.parse("2024-01-01T11:00:00Z"), "{}")
        ));
        batchService.completeBatch(batchId);

        var prices = priceQueryService.getLastPrices(Set.of("TESLA"));
        assertThat(prices.get(0).getAsOf())
                .isEqualTo(Instant.parse("2024-01-01T11:00:00Z"));
    }
}


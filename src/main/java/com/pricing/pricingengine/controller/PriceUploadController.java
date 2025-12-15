package com.pricing.pricingengine.controller;

import com.pricing.pricingengine.dto.PriceRecord;
import com.pricing.pricingengine.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/batches")
@Tag(name = "A) PRICES UPLOAD API", description = "ENDPOINT FOR UPLOADING PRICES IN BATCHES WITH COMPLETE/CANCEL ACTION.")
public class PriceUploadController {

    private static final Logger log = LoggerFactory.getLogger(PriceUploadController.class);

    private static final int CHUNK_SIZE = 1000;

    private final BatchService batchService;

    public PriceUploadController(BatchService batchService) {
        this.batchService = batchService;
    }

    /**
     * Uploads a file of prices into a batch.
     * The batch must be explicitly completed via a separate call.
     */
    @Operation(
            summary = "UPLOAD PRICES FILE AND PROCESS IN CHUNKS OF 1000"
    )
    @PostMapping(
            value = "/{batchId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> uploadPrices(
            @PathVariable String batchId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        log.info("Received upload request for batch {} file={}", batchId, file.getOriginalFilename());

        // Idempotent start (safe if already exists)
        batchService.startBatch(batchId);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            List<PriceRecord> buffer = new ArrayList<>(CHUNK_SIZE);
            String line;

            while ((line = reader.readLine()) != null) {
                PriceRecord record = parseCsvLine(line);
                buffer.add(record);

                if (buffer.size() == CHUNK_SIZE) {
                    batchService.uploadPrices(batchId, buffer);
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                batchService.uploadPrices(batchId, buffer);
            }
        }

        log.info("Upload completed for batch {}", batchId);

        return ResponseEntity.accepted()
                .body("Upload accepted for batch " + batchId +
                        ". Call /complete to make data visible.");
    }

    /**
     * Explicit batch completion.
     */
    @Operation(
            summary = "BATCH COMPLETION BY BATCHID"
    )
    @PostMapping("/{batchId}/complete")
    public ResponseEntity<String> completeBatch(@PathVariable String batchId) {
        batchService.completeBatch(batchId);
        return ResponseEntity.ok("Batch completed: " + batchId);
    }

    /**
     * Explicit batch cancellation.
     */
    @Operation(
            summary = "BATCH CANCELLATION BY BATCHID"
    )
    @PostMapping("/{batchId}/cancel")
    public ResponseEntity<String> cancelBatch(@PathVariable String batchId) {
        batchService.cancelBatch(batchId);
        return ResponseEntity.ok("Batch cancelled: " + batchId);
    }

    /**
     * CSV format:
     * instrumentId,asOfIsoInstant,payloadJson
     */
    private PriceRecord parseCsvLine(String line) {
        String[] parts = line.split(",", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid CSV line: " + line);
        }

        return new PriceRecord(
                parts[0].trim(),
                Instant.parse(parts[1].trim()),
                parts[2].trim()
        );
    }
}


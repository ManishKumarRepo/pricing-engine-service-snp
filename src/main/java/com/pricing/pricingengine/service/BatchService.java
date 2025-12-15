package com.pricing.pricingengine.service;

import com.pricing.pricingengine.domain.BatchEntity;
import com.pricing.pricingengine.domain.BatchStatus;
import com.pricing.pricingengine.domain.PriceEntity;
import com.pricing.pricingengine.dto.PriceRecord;
import com.pricing.pricingengine.lockmanager.BatchLockManager;
import com.pricing.pricingengine.repository.BatchRepository;
import com.pricing.pricingengine.repository.PriceRepository;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.Lock;

@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private final BatchRepository batchRepo;
    private final PriceRepository priceRepo;
    private final BatchLockManager lockManager;

    public BatchService(BatchRepository batchRepo,
                        PriceRepository priceRepo,
                        BatchLockManager lockManager) {
        this.batchRepo = batchRepo;
        this.priceRepo = priceRepo;
        this.lockManager = lockManager;
    }

    @Transactional
    public void startBatch(String batchId) {
        if (batchRepo.existsById(batchId)) {
            throw new IllegalStateException("Batch already exists");
        }
        batchRepo.save(new BatchEntity(batchId));
        log.info("Batch {} started", batchId);
    }

    @Transactional
    public void uploadPrices(String batchId, List<PriceRecord> records) {
        Lock lock = lockManager.lock(batchId);
        try {
            BatchEntity batch = batchRepo.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Batch not found"));

            if (batch.getStatus() != BatchStatus.STARTED) {
                throw new IllegalStateException("Batch not STARTED");
            }

            List<PriceEntity> entities = records.stream()
                    .map(r -> new PriceEntity(r.instrumentId(), r.asOf(), r.payloadJson(), batch))
                    .toList();

            priceRepo.saveAll(entities);
            priceRepo.flush();

            log.info("Uploaded {} prices for batch {}", records.size(), batchId);
        } finally {
            lockManager.unlock(batchId, lock);
        }
    }

    @Transactional
    public void completeBatch(String batchId) {
        Lock lock = lockManager.lock(batchId);
        try {
            BatchEntity batch = batchRepo.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Batch not found"));

            batch.complete();
            log.info("Batch {} completed", batchId);
        } finally {
            lockManager.unlock(batchId, lock);
        }
    }

    @Transactional
    public void cancelBatch(String batchId) {
        Lock lock = lockManager.lock(batchId);
        try {
            BatchEntity batch = batchRepo.findById(batchId).orElseThrow();
            batch.cancel();
            priceRepo.deleteByBatch(batch);
            log.warn("Batch {} cancelled", batchId);
        } finally {
            lockManager.unlock(batchId, lock);
        }
    }
}

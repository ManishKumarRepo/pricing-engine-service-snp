package com.pricing.pricingengine.repository;

import com.pricing.pricingengine.domain.BatchEntity;
import com.pricing.pricingengine.domain.PriceEntity;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PriceRepository extends JpaRepository<PriceEntity, Long> {

    @Query("""
    SELECT p FROM PriceEntity p
    JOIN p.batch b
    WHERE p.instrumentId IN :ids AND b.status = 'COMPLETED'
      AND p.asOf = (SELECT MAX(p2.asOf) FROM PriceEntity p2 JOIN p2.batch b2
                    WHERE p2.instrumentId = p.instrumentId AND b2.status = 'COMPLETED')
    """)
    List<PriceEntity> findLastPrices(Set<String> ids);

    void deleteByBatch(BatchEntity batch);
}


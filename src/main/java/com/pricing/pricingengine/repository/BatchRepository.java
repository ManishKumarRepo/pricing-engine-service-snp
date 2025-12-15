package com.pricing.pricingengine.repository;

import com.pricing.pricingengine.domain.BatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<BatchEntity, String> {
}


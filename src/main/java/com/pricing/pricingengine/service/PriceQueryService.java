package com.pricing.pricingengine.service;

import com.pricing.pricingengine.domain.PriceEntity;
import com.pricing.pricingengine.repository.PriceRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PriceQueryService {

    private final PriceRepository repo;

    public PriceQueryService(PriceRepository repo) {
        this.repo = repo;
    }

    public List<PriceEntity> getLastPrices(Set<String> instrumentIds) {
        return repo.findLastPrices(instrumentIds);
    }
}

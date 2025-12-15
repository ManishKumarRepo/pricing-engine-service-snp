package com.pricing.pricingengine.lockmanager;

import org.springframework.stereotype.Component;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

@Component
public class BatchLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Lock lock(String batchId) {
        ReentrantLock lock = locks.computeIfAbsent(batchId, id -> new ReentrantLock());
        lock.lock(); // block until acquired
        return lock;
    }

    public void unlock(String batchId, Lock lock) {
        ReentrantLock reentrantLock = (ReentrantLock) lock;
        try {
            reentrantLock.unlock();
        } finally {
            // remove only if no thread is holding or waiting
            if (!reentrantLock.isLocked() && !reentrantLock.hasQueuedThreads()) {
                locks.remove(batchId, reentrantLock);
            }
        }
    }
}

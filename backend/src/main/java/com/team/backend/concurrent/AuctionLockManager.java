package com.team.backend.concurrent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class AuctionLockManager {

    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();

    public Lock getLock(Long auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }
}
package com.team.backend.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionLockManager {

    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();
    // key value => key = auction ID , value= lock

    public Lock getLock(Long auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
        // computeIfAbsent: auction nao chua có lock thì tạo lock mới , có roi thì trả về lock cũ
        // ReentrantLock: kiem soat truy cap dong thoi

        /* Tai sao khong su dung synchronized ?
        Co the dung , nhung viec su dung cai ReentrantLock nay se linh hoat hon:
        + lock theo tung Auction ID
        + khong khoa toan bo cai service
        + hieu nang tot hon khi co nhieu auction no chay cung 1 luc
         */
    }
}
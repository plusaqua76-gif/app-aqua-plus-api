package com.aqua.plus.api.utils;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class NonceStore {

    static final long TTL_MS = 5 * 60 * 1000L;
    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    public boolean registerIfNew(String nonce) {
        long now = System.currentTimeMillis();
        return store.putIfAbsent(nonce, now) == null;
    }

    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        long limit = System.currentTimeMillis() - TTL_MS;
        store.entrySet().removeIf(e -> e.getValue() < limit);
    }

    int size() {
        return store.size();
    }
}

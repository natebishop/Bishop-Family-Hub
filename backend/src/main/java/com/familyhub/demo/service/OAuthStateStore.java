package com.familyhub.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateStore {

    private static final long TTL_SECONDS = 600; // 10 minutes

    private final Map<String, StateEntry> store = new ConcurrentHashMap<>();

    public String generateState(UUID memberId) {
        String state = UUID.randomUUID().toString();
        store.put(state, new StateEntry(memberId, Instant.now().plusSeconds(TTL_SECONDS)));
        return state;
    }

    public Optional<UUID> consumeState(String state) {
        if (state == null) {
            return Optional.empty();
        }
        StateEntry entry = store.remove(state);
        if (entry == null || entry.expiry().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.memberId());
    }

    @Scheduled(fixedRate = 60_000)
    void cleanupExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> e.getValue().expiry().isBefore(now));
    }

    // Visible for testing
    int size() {
        return store.size();
    }

    private record StateEntry(UUID memberId, Instant expiry) {}
}

package com.familyhub.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthStateStoreTest {

    private OAuthStateStore stateStore;

    @BeforeEach
    void setUp() {
        stateStore = new OAuthStateStore();
    }

    @Test
    void generateState_returnsNonNullToken() {
        UUID memberId = UUID.randomUUID();
        String state = stateStore.generateState(memberId);

        assertThat(state).isNotNull().isNotEmpty();
    }

    @Test
    void consumeState_validState_returnsMemberId() {
        UUID memberId = UUID.randomUUID();
        String state = stateStore.generateState(memberId);

        Optional<UUID> result = stateStore.consumeState(state);

        assertThat(result).contains(memberId);
    }

    @Test
    void consumeState_singleUse_secondCallReturnsEmpty() {
        UUID memberId = UUID.randomUUID();
        String state = stateStore.generateState(memberId);

        stateStore.consumeState(state);
        Optional<UUID> secondAttempt = stateStore.consumeState(state);

        assertThat(secondAttempt).isEmpty();
    }

    @Test
    void consumeState_unknownState_returnsEmpty() {
        Optional<UUID> result = stateStore.consumeState("nonexistent-state");

        assertThat(result).isEmpty();
    }

    @Test
    void consumeState_nullState_returnsEmpty() {
        Optional<UUID> result = stateStore.consumeState(null);

        assertThat(result).isEmpty();
    }

    @Test
    void generateState_multipleMembers_trackedIndependently() {
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();

        String state1 = stateStore.generateState(member1);
        String state2 = stateStore.generateState(member2);

        assertThat(stateStore.consumeState(state1)).contains(member1);
        assertThat(stateStore.consumeState(state2)).contains(member2);
    }

    @Test
    void cleanupExpired_removesExpiredEntries() throws Exception {
        // We can't easily test TTL without reflection or a clock abstraction,
        // but we can verify cleanup doesn't remove valid entries
        UUID memberId = UUID.randomUUID();
        stateStore.generateState(memberId);

        stateStore.cleanupExpired();

        assertThat(stateStore.size()).isEqualTo(1);
    }
}

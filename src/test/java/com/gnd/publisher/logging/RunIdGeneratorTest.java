package com.gnd.publisher.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RunIdGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void generatesRunIdWithStagePrefixAndTimestamp() {
        String runId = RunIdGenerator.generate("ingestion", CLOCK);

        assertThat(runId).startsWith("ingestion-" + NOW + "-");
        assertThat(runId).matches("ingestion-.+-[0-9a-f]{8}");
    }

    @Test
    void generatesDistinctIdsOnSuccessiveCalls() {
        String first = RunIdGenerator.generate("cleanup", CLOCK);
        String second = RunIdGenerator.generate("cleanup", CLOCK);

        assertThat(first).isNotEqualTo(second);
    }
}

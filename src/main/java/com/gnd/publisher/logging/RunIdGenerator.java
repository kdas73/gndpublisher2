package com.gnd.publisher.logging;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class RunIdGenerator {

    private RunIdGenerator() {
    }

    public static String generate(String stage, Clock clock) {
        return stage + "-" + Instant.now(clock) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

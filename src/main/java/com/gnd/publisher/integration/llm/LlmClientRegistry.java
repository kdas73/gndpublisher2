package com.gnd.publisher.integration.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gnd.publisher.exception.LlmIntegrationException;

import org.springframework.stereotype.Component;

/**
 * Resolves an {@link LlmClient} by configured provider id, so that provider selection stays data
 * rather than bean wiring. Every adapter on the classpath is registered; configuration picks one
 * per use case.
 */
@Component
public class LlmClientRegistry {

    private final Map<String, LlmClient> clientsByProviderId;

    public LlmClientRegistry(List<LlmClient> clients) {
        Map<String, LlmClient> byProviderId = new LinkedHashMap<>();
        for (LlmClient client : clients) {
            LlmClient previous = byProviderId.put(normalize(client.providerId()), client);
            if (previous != null) {
                throw new IllegalStateException("Duplicate LLM provider id: " + client.providerId());
            }
        }
        this.clientsByProviderId = Map.copyOf(byProviderId);
    }

    public LlmClient require(String providerId) {
        return Optional.ofNullable(clientsByProviderId.get(normalize(providerId)))
                .orElseThrow(() -> new LlmIntegrationException("Unknown LLM provider id: " + providerId
                        + ". Known provider ids: " + providerIds()));
    }

    public Set<String> providerIds() {
        return clientsByProviderId.keySet();
    }

    private static String normalize(String providerId) {
        return providerId == null ? "" : providerId.trim().toLowerCase(Locale.ROOT);
    }
}

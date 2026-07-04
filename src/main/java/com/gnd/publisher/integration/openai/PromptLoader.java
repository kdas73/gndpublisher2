package com.gnd.publisher.integration.openai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gnd.publisher.exception.OpenAiIntegrationException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PromptLoader {

    private static final String PROMPT_PATH_PREFIX = "prompts/";
    private static final String PROMPT_PATH_SUFFIX = ".txt";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String load(String promptVersion) {
        if (promptVersion == null || promptVersion.isBlank()) {
            throw new OpenAiIntegrationException("Prompt version must not be blank");
        }
        return cache.computeIfAbsent(promptVersion, this::loadUncached);
    }

    private String loadUncached(String promptVersion) {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH_PREFIX + promptVersion + PROMPT_PATH_SUFFIX);
        if (!resource.exists()) {
            throw new OpenAiIntegrationException("Prompt resource not found for version: " + promptVersion);
        }

        try (var inputStream = resource.getInputStream()) {
            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).strip();
            if (prompt.isBlank()) {
                throw new OpenAiIntegrationException("Prompt resource is blank for version: " + promptVersion);
            }
            return prompt;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load prompt version: " + promptVersion, exception);
        }
    }
}

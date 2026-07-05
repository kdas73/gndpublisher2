package com.gnd.publisher.integration.openai;

import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;

import org.springframework.stereotype.Component;

@Component
public class OpenAiCategorizer {

    private final OpenAiClient openAiClient;

    public OpenAiCategorizer(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public CategorizationResult classify(CategoryClassificationRequest request) {
        OpenAiClient.ClassificationResult result = openAiClient.classify(request);
        return new CategorizationResult(result.response(), result.rawResponse(), result.model());
    }

    public record CategorizationResult(
            CategoryClassificationResponse response,
            String rawResponse,
            String model) {
    }
}

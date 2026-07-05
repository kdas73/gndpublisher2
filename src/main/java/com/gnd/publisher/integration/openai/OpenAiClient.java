package com.gnd.publisher.integration.openai;

import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.PublicationContentRequest;
import com.gnd.publisher.dto.openai.PublicationContentResponse;

public interface OpenAiClient {

    ClassificationResult classify(CategoryClassificationRequest request);

    PublicationContentResponse preparePublicationContent(PublicationContentRequest request);

    record ClassificationResult(
            CategoryClassificationResponse response,
            String rawResponse,
            String model) {
    }
}

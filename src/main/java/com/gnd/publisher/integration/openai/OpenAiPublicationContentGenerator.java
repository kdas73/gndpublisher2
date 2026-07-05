package com.gnd.publisher.integration.openai;

import com.gnd.publisher.dto.openai.PublicationContentRequest;
import com.gnd.publisher.dto.openai.PublicationContentResponse;

import org.springframework.stereotype.Component;

@Component
public class OpenAiPublicationContentGenerator {

    private final OpenAiClient openAiClient;

    public OpenAiPublicationContentGenerator(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public PublicationContentResponse prepare(PublicationContentRequest request) {
        return openAiClient.preparePublicationContent(request);
    }
}

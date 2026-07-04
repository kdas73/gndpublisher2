package com.gnd.publisher.integration.openai;

import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.PublicationContentRequest;
import com.gnd.publisher.dto.openai.PublicationContentResponse;

public interface OpenAiClient {

    CategoryClassificationResponse classify(CategoryClassificationRequest request);

    PublicationContentResponse preparePublicationContent(PublicationContentRequest request);
}

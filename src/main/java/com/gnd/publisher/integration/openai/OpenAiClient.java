package com.gnd.publisher.integration.openai;

import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.SummaryRequest;
import com.gnd.publisher.dto.openai.SummaryResponse;
import com.gnd.publisher.dto.openai.TranslationRequest;
import com.gnd.publisher.dto.openai.TranslationResponse;

public interface OpenAiClient {

    CategoryClassificationResponse classify(CategoryClassificationRequest request);

    SummaryResponse summarize(SummaryRequest request);

    TranslationResponse translate(TranslationRequest request);
}

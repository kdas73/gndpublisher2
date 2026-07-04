package com.gnd.publisher.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.SemanticKeyActionDto;
import com.gnd.publisher.dto.openai.SummaryRequest;
import com.gnd.publisher.dto.openai.SummaryResponse;
import com.gnd.publisher.dto.openai.TranslationRequest;
import com.gnd.publisher.dto.openai.TranslationResponse;

import org.junit.jupiter.api.Test;

class OpenAiJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void deserializesClassificationFixtures() throws Exception {
        CategoryClassificationRequest request = read("classification-request.json", CategoryClassificationRequest.class);
        CategoryClassificationResponse response = read("classification-response.json", CategoryClassificationResponse.class);

        assertThat(request.newsItem().title()).isEqualTo("Greek parliament approves new migration bill");
        assertThat(request.categoryOptions()).extracting(option -> option.code()).containsExactly("politics", "weather");
        assertThat(response.semanticKeyAction()).isEqualTo(SemanticKeyActionDto.MATCHED_EXISTING);
        assertThat(response.matchedSemanticEventId()).isEqualTo("event-456");
    }

    @Test
    void deserializesSummaryFixtures() throws Exception {
        SummaryRequest request = read("summary-request.json", SummaryRequest.class);
        SummaryResponse response = read("summary-response.json", SummaryResponse.class);

        assertThat(request.maxCharacters()).isEqualTo(600);
        assertThat(response.confidence()).isEqualTo(0.9);
    }

    @Test
    void deserializesTranslationFixtures() throws Exception {
        TranslationRequest request = read("translation-request.json", TranslationRequest.class);
        TranslationResponse response = read("translation-response.json", TranslationResponse.class);

        assertThat(request.targetLanguage()).isEqualTo("en");
        assertThat(response.summary()).contains("parliamentary debate");
    }

    private <T> T read(String fileName, Class<T> type) throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/fixtures/openai/" + fileName)) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), type);
        }
    }
}

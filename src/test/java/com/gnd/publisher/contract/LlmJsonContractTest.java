package com.gnd.publisher.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryClassificationResponse;
import com.gnd.publisher.dto.llm.PublicationContentRequest;
import com.gnd.publisher.dto.llm.PublicationContentResponse;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;

import org.junit.jupiter.api.Test;

class LlmJsonContractTest {

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
    void deserializesPublicationContentFixtures() throws Exception {
        PublicationContentRequest request = read("publication-content-request.json", PublicationContentRequest.class);
        PublicationContentResponse response = read("publication-content-response.json", PublicationContentResponse.class);

        assertThat(request.targetLanguage()).isEqualTo("en");
        assertThat(request.semanticKey()).contains("migration bill");
        assertThat(request.maxSummaryCharacters()).isEqualTo(600);
        assertThat(response.summary()).contains("parliamentary debate");
    }

    private <T> T read(String fileName, Class<T> type) throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/fixtures/llm/" + fileName)) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), type);
        }
    }
}

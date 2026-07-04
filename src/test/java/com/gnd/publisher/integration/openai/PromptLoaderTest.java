package com.gnd.publisher.integration.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gnd.publisher.exception.OpenAiIntegrationException;

import org.junit.jupiter.api.Test;

class PromptLoaderTest {

    private final PromptLoader promptLoader = new PromptLoader();

    @Test
    void loadsPromptTemplatesFromResources() {
        assertThat(promptLoader.load("classification-v1")).contains("classify Greek news items");
        assertThat(promptLoader.load("publication-content-v1")).contains("target-language publication content");
    }

    @Test
    void cachesLoadedPromptText() {
        String first = promptLoader.load("classification-v1");
        String second = promptLoader.load("classification-v1");

        assertThat(second).isSameAs(first);
    }

    @Test
    void rejectsBlankPromptVersion() {
        assertThatThrownBy(() -> promptLoader.load(" "))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejectsMissingPromptVersion() {
        assertThatThrownBy(() -> promptLoader.load("missing-v1"))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void rejectsBlankPromptResource() {
        assertThatThrownBy(() -> promptLoader.load("blank-v1"))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("blank");
    }
}

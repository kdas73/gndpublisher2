package com.gnd.publisher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.integration.llm.JdkLlmHttpSender;
import com.gnd.publisher.integration.llm.LlmClient;
import com.gnd.publisher.integration.llm.LlmHttpSender;
import com.gnd.publisher.integration.llm.ollama.OllamaLlmClient;
import com.gnd.publisher.integration.llm.openai.OpenAiLlmClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root for the LLM integration layer. Every adapter is registered unconditionally;
 * {@code gnd.llm.<use-case>.provider} decides which one each use case resolves at runtime, so
 * switching providers is configuration rather than wiring.
 *
 * <p>Each provider gets its own {@link LlmHttpSender}, because the connect timeout is configured on
 * the JDK HTTP client rather than per request.
 */
@Configuration(proxyBeanMethods = false)
public class LlmClientsConfiguration {

    @Bean
    public LlmHttpSender openAiHttpSender(LlmProperties properties) {
        return new JdkLlmHttpSender(properties.openai().timeouts().connect());
    }

    @Bean
    public LlmHttpSender ollamaHttpSender(LlmProperties properties) {
        return new JdkLlmHttpSender(properties.ollama().timeouts().connect());
    }

    @Bean
    public LlmClient openAiLlmClient(
            LlmProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("openAiHttpSender") LlmHttpSender httpSender) {
        return new OpenAiLlmClient(properties.openai(), objectMapper, httpSender);
    }

    @Bean
    public LlmClient ollamaLlmClient(
            LlmProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("ollamaHttpSender") LlmHttpSender httpSender) {
        return new OllamaLlmClient(properties.ollama(), objectMapper, httpSender);
    }
}

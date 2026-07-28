package com.gnd.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ObservabilityHealthEndpointTest {

    private static final Path DATABASE_FILE = Path.of(System.getProperty("java.io.tmpdir"),
            "gnd-observability-health-test-" + UUID.randomUUID() + ".sqlite3");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_FILE);
        registry.add("gnd.openai.api-key", () -> "test-api-key");
        registry.add("gnd.telegram.bot-token", () -> "test-bot-token");
        registry.add("gnd.telegram.channels[0].code", () -> "news-ru");
        registry.add("gnd.telegram.channels[0].language", () -> "ru");
        registry.add("gnd.telegram.channels[0].channel-id", () -> "-1000000000001");
        registry.add("gnd.telegram.channels[0].username", () -> "gnd_news_test");
        registry.add("gnd.telegram.channels[0].message-url-template", () -> "https://t.me/{username}/{messageId}");
        registry.add("gnd.telegram.channels[0].enabled", () -> "true");
        registry.add("gnd.telegram.channels[1].code", () -> "important-news-ru");
        registry.add("gnd.telegram.channels[1].language", () -> "ru");
        registry.add("gnd.telegram.channels[1].channel-id", () -> "-1000000000002");
        registry.add("gnd.telegram.channels[1].username", () -> "gnd_important_news_test");
        registry.add("gnd.telegram.channels[1].message-url-template", () -> "https://t.me/{username}/{messageId}");
        registry.add("gnd.telegram.channels[1].enabled", () -> "true");
    }

    @AfterAll
    static void deleteDatabaseFile() {
        DATABASE_FILE.toFile().delete();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void livenessProbeIsUp() {
        assertUp("/actuator/health/liveness");
    }

    @Test
    void readinessProbeIsUp() {
        assertUp("/actuator/health/readiness");
    }

    @Test
    void metricsEndpointIsExposed() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private void assertUp(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}

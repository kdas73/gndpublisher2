package com.gnd.publisher.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ConfigurationPropertiesBindingTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void bindsRepresentativeRuntimeConfiguration() {
        assertValid(bind("gnd", RuntimeProfileProperties.class, Map.of("gnd.profile", "local")));

        CategoryProperties categories = bind("gnd.categories", CategoryProperties.class, categoryProperties());
        assertValid(categories);
        assertThat(categories.publishableCodes()).containsExactly("politics", "weather");

        PublishingProperties publishing = bind("gnd.publishing", PublishingProperties.class, publishingProperties());
        assertValid(publishing);
        assertThat(publishing.targetLanguages()).containsExactly("en");

        ImportantNewsDigestProperties digest = bind(
                "gnd.important-news-digest",
                ImportantNewsDigestProperties.class,
                importantNewsDigestProperties());
        assertValid(digest);

        OpenAiProperties openAi = bind("gnd.openai", OpenAiProperties.class, openAiProperties("test-api-key"));
        assertValid(openAi);
        assertThat(openAi.models().categorization()).isEqualTo("gpt-5.4-mini");

        TelegramProperties telegram = bind("gnd.telegram", TelegramProperties.class, telegramProperties("test-bot-token"));
        assertValid(telegram);
        assertThat(telegram.channels()).extracting(TelegramProperties.Channel::language).containsOnly("en");

        SchedulerProperties scheduler = bind("gnd.scheduler", SchedulerProperties.class, schedulerProperties());
        assertValid(scheduler);

        CleanupProperties cleanup = bind("gnd.cleanup", CleanupProperties.class, cleanupProperties("30d"));
        assertValid(cleanup);
    }

    @Test
    void rejectsMissingOpenAiApiKey() {
        OpenAiProperties properties = bind("gnd.openai", OpenAiProperties.class, openAiProperties(""));

        assertInvalidProperty(properties, "apiKey");
    }

    @Test
    void rejectsMissingTelegramBotToken() {
        TelegramProperties properties = bind("gnd.telegram", TelegramProperties.class, telegramProperties(""));

        assertInvalidProperty(properties, "botToken");
    }

    @Test
    void rejectsEmptyCategoryOptions() {
        Map<String, String> values = categoryProperties();
        values.keySet().removeIf(key -> key.startsWith("gnd.categories.options"));

        CategoryProperties properties = bind("gnd.categories", CategoryProperties.class, values);

        assertInvalidProperty(properties, "options");
    }

    @Test
    void rejectsEmptyPublishableCategoryCodes() {
        Map<String, String> values = categoryProperties();
        values.keySet().removeIf(key -> key.startsWith("gnd.categories.publishable-codes"));

        CategoryProperties properties = bind("gnd.categories", CategoryProperties.class, values);

        assertInvalidProperty(properties, "publishableCodes");
    }

    @Test
    void rejectsMissingEditorialRulesVersion() {
        Map<String, String> values = openAiProperties("test-api-key");
        values.remove("gnd.openai.prompts.editorial-rules-version");

        OpenAiProperties properties = bind("gnd.openai", OpenAiProperties.class, values);

        assertInvalidProperty(properties, "prompts.editorialRulesVersion");
    }

    @Test
    void rejectsInvalidPublishingQuota() {
        Map<String, String> values = publishingProperties();
        values.put("gnd.publishing.default-max-items-per-source-per-run", "0");

        PublishingProperties properties = bind("gnd.publishing", PublishingProperties.class, values);

        assertInvalidProperty(properties, "defaultMaxItemsPerSourcePerRun");
    }

    @Test
    void rejectsMissingTelegramChannelMappings() {
        Map<String, String> values = telegramProperties("test-bot-token");
        values.keySet().removeIf(key -> key.startsWith("gnd.telegram.channels"));

        TelegramProperties properties = bind("gnd.telegram", TelegramProperties.class, values);

        assertInvalidProperty(properties, "channels");
    }

    @Test
    void rejectsInvalidCleanupRetention() {
        CleanupProperties properties = bind("gnd.cleanup", CleanupProperties.class, cleanupProperties("0d"));

        assertInvalidProperty(properties, "positiveRetentionPeriods");
    }

    private static <T> T bind(String prefix, Class<T> propertiesType, Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind(prefix, Bindable.of(propertiesType))
                .orElseThrow(() -> new AssertionError("Expected properties to bind for prefix " + prefix));
    }

    private static void assertValid(Object properties) {
        assertThat(VALIDATOR.validate(properties)).isEmpty();
    }

    private static void assertInvalidProperty(Object properties, String expectedPropertyPath) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(properties);

        assertThat(violations)
                .describedAs("Validation paths for %s", properties)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(expectedPropertyPath);
    }

    private static Map<String, String> categoryProperties() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.categories.options[0].code", "politics");
        values.put("gnd.categories.options[0].name", "Politics");
        values.put("gnd.categories.options[0].description", "Government and elections");
        values.put("gnd.categories.options[0].publication-priority", "10");
        values.put("gnd.categories.options[0].enabled", "true");
        values.put("gnd.categories.options[1].code", "weather");
        values.put("gnd.categories.options[1].name", "Weather");
        values.put("gnd.categories.options[1].description", "Weather alerts");
        values.put("gnd.categories.options[1].publication-priority", "20");
        values.put("gnd.categories.options[1].enabled", "true");
        values.put("gnd.categories.publishable-codes[0]", "politics");
        values.put("gnd.categories.publishable-codes[1]", "weather");
        return values;
    }

    private static Map<String, String> publishingProperties() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.publishing.target-languages[0]", "en");
        values.put("gnd.publishing.default-max-items-per-source-per-run", "3");
        values.put("gnd.publishing.source-quota-overrides.kathimerini", "2");
        values.put("gnd.publishing.summary-max-characters", "600");
        values.put("gnd.publishing.telegram-max-message-characters", "3500");
        values.put("gnd.publishing.digest.enabled", "true");
        values.put("gnd.publishing.digest.duplicate-threshold", "2");
        values.put("gnd.publishing.digest.channel-routing-key", "important-news");
        return values;
    }

    private static Map<String, String> importantNewsDigestProperties() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.important-news-digest.enabled", "true");
        values.put("gnd.important-news-digest.cron", "0 0 */6 * * *");
        values.put("gnd.important-news-digest.duplicate-threshold", "2");
        values.put("gnd.important-news-digest.target-language", "en");
        values.put("gnd.important-news-digest.channel-code", "important-news-en");
        values.put("gnd.important-news-digest.lookback-window", "7d");
        return values;
    }

    private static Map<String, String> openAiProperties(String apiKey) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.openai.api-key", apiKey);
        values.put("gnd.openai.models.categorization", "gpt-5.4-mini");
        values.put("gnd.openai.models.publication-content", "gpt-5.5");
        values.put("gnd.openai.prompts.classification-version", "classification-v1");
        values.put("gnd.openai.prompts.editorial-rules-version", "editorial-rules-v1");
        values.put("gnd.openai.prompts.publication-content-version", "publication-content-v1");
        values.put("gnd.openai.timeouts.connect", "5s");
        values.put("gnd.openai.timeouts.read", "60s");
        values.put("gnd.openai.semantic-event-lookup-window", "3d");
        return values;
    }

    private static Map<String, String> telegramProperties(String botToken) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.telegram.bot-token", botToken);
        values.put("gnd.telegram.channels[0].code", "news-en");
        values.put("gnd.telegram.channels[0].language", "en");
        values.put("gnd.telegram.channels[0].channel-id", "-1001234567890");
        values.put("gnd.telegram.channels[0].username", "gnd_news_en");
        values.put("gnd.telegram.channels[0].message-url-template", "https://t.me/{username}/{messageId}");
        values.put("gnd.telegram.channels[0].enabled", "true");
        return values;
    }

    private static Map<String, String> schedulerProperties() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.scheduler.ingestion.enabled", "true");
        values.put("gnd.scheduler.ingestion.cron", "0 */3 * * * *");
        values.put("gnd.scheduler.publication.enabled", "true");
        values.put("gnd.scheduler.publication.cron", "0 */20 * * * *");
        values.put("gnd.scheduler.important-news-digest.enabled", "true");
        values.put("gnd.scheduler.important-news-digest.cron", "0 0 */6 * * *");
        values.put("gnd.scheduler.cleanup.enabled", "true");
        values.put("gnd.scheduler.cleanup.cron", "0 0 3 * * *");
        return values;
    }

    private static Map<String, String> cleanupProperties(String newsItemsRetention) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gnd.cleanup.news-items-retention", newsItemsRetention);
        values.put("gnd.cleanup.classification-runs-retention", "30d");
        values.put("gnd.cleanup.publications-retention", "180d");
        values.put("gnd.cleanup.digest-retention", "180d");
        return values;
    }
}

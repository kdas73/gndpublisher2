package com.gnd.publisher.mapper;

import java.util.Optional;

import com.gnd.publisher.config.PublishingProperties;
import com.gnd.publisher.domain.model.Category;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.domain.model.Translation;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;

import org.springframework.stereotype.Component;

@Component
public class TelegramMessageMapper {

    private static final String PARSE_MODE_HTML = "HTML";
    private static final String SOURCE_PREFIX = "\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a: ";
    private static final String TOPIC_PREFIX = "\u0422\u0435\u043c\u0430: ";
    private static final String ELLIPSIS = "...";

    private final PublishingProperties publishingProperties;

    public TelegramMessageMapper(PublishingProperties publishingProperties) {
        this.publishingProperties = publishingProperties;
    }

    public TelegramMessageDto toMessage(Translation translation, TelegramChannel channel) {
        String sourceUrl = translation.getNewsItem().getSourceUrl();
        String title = link(translation.getTitle().strip(), sourceUrl);
        String summary = escapeHtml(translation.getSummary().strip());
        String attribution = SOURCE_PREFIX + link(translation.getNewsItem().getSource().getName(), sourceUrl);
        String topic = topic(translation);
        String message = fit(title, summary, attribution, topic);
        return new TelegramMessageDto(channel.getChannelId(), message, PARSE_MODE_HTML);
    }

    private String fit(String title, String summary, String attribution, String topic) {
        int maxLength = publishingProperties.telegramMaxMessageCharacters();
        String message = format(title, summary, attribution, topic);
        if (message.length() <= maxLength) {
            return message;
        }

        int fixedLength = format(title, "", attribution, topic).length() + 2;
        int summaryLimit = Math.max(0, maxLength - fixedLength);
        return format(title, truncate(summary, summaryLimit), attribution, topic);
    }

    private String format(String title, String summary, String attribution, String topic) {
        String sourceBlock = attribution + (topic.isBlank() ? "" : "\n" + topic);
        if (summary.isBlank()) {
            return title + "\n\n" + sourceBlock;
        }
        return title + "\n\n" + summary + "\n\n" + sourceBlock;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= ELLIPSIS.length()) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - ELLIPSIS.length()).stripTrailing() + ELLIPSIS;
    }

    private String link(String text, String url) {
        return "<a href=\"" + escapeHtml(url) + "\">" + escapeHtml(text) + "</a>";
    }

    private String topic(Translation translation) {
        return Optional.ofNullable(translation.getSemanticEvent().getCategory())
                .map(Category::getCode)
                .map(value -> TOPIC_PREFIX + escapeHtml(value))
                .orElse("");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

package com.gnd.publisher.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.gnd.publisher.domain.model.ImportantNewsDigestItem;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;

import org.springframework.stereotype.Component;

@Component
public class ImportantNewsDigestMessageMapper {

    private static final String PARSE_MODE_HTML = "HTML";
    private static final String HEADER =
            "\u0412\u0430\u0436\u043d\u044b\u0435 \u043d\u043e\u0432\u043e\u0441\u0442\u0438:";

    public TelegramMessageDto toMessage(List<ImportantNewsDigestItem> items, TelegramChannel channel) {
        String body = items.stream()
                .map(item -> link(item.getTitle().strip(), item.getPublicationUrl()))
                .collect(Collectors.joining("\n"));
        String message = HEADER + "\n\n" + body;
        return new TelegramMessageDto(channel.getChannelId(), message, PARSE_MODE_HTML);
    }

    private String link(String text, String url) {
        return "<a href=\"" + escapeHtml(url) + "\">" + escapeHtml(text) + "</a>";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

package com.gnd.publisher.integration.rss;

import java.io.StringReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.gnd.publisher.dto.rss.RssFeedItemDto;
import com.gnd.publisher.exception.FeedReadException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class RssFeedParser {

    public List<RssFeedItemDto> parse(String xml) {
        Document document = parseDocument(xml);
        NodeList itemNodes = document.getElementsByTagName("item");
        List<RssFeedItemDto> items = new ArrayList<>();

        for (int index = 0; index < itemNodes.getLength(); index++) {
            Element itemElement = (Element) itemNodes.item(index);
            textOf(itemElement, "title")
                    .map(title -> new RssFeedItemDto(
                            textOf(itemElement, "guid"),
                            title,
                            textOf(itemElement, "link"),
                            textOf(itemElement, "description"),
                            textOf(itemElement, "pubDate").flatMap(this::parsePublishedAt)))
                    .ifPresent(items::add);
        }

        return items;
    }

    private Document parseDocument(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            disableExternalEntities(factory);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception exception) {
            throw new FeedReadException("Failed to parse RSS feed", exception);
        }
    }

    private void disableExternalEntities(DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    }

    private Optional<Instant> parsePublishedAt(String value) {
        return parseDate(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                .or(() -> parseDate(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    private Optional<Instant> parseDate(String value, DateTimeFormatter formatter) {
        try {
            return Optional.of(OffsetDateTime.parse(value, formatter).toInstant());
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private Optional<String> textOf(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return Optional.empty();
        }
        Node node = nodes.item(0);
        String text = node.getTextContent();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text.trim());
    }
}

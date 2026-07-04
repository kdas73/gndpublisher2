package com.gnd.publisher.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class UrlNormalizer {

    private static final String EMPTY = "";

    public String normalize(String url) {
        String trimmed = Optional.ofNullable(url)
                .map(String::trim)
                .orElse(EMPTY);
        if (trimmed.isEmpty()) {
            return EMPTY;
        }

        try {
            URI uri = new URI(trimmed);
            String scheme = Optional.ofNullable(uri.getScheme())
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .orElse(null);
            String host = Optional.ofNullable(uri.getHost())
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .orElse(null);
            int port = normalizePort(scheme, uri.getPort());
            String path = normalizePath(uri);
            String query = normalizeQuery(uri.getRawQuery());

            return buildUrl(scheme, uri.getRawUserInfo(), host, port, path, query);
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return trimmed;
        }
    }

    private int normalizePort(String scheme, int port) {
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            return -1;
        }
        return port;
    }

    private String normalizePath(URI uri) {
        String rawPath = Optional.ofNullable(uri.getRawPath()).orElse(EMPTY);
        try {
            return URI.create(rawPath).normalize().getRawPath();
        } catch (IllegalArgumentException exception) {
            return rawPath;
        }
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        String normalized = Arrays.stream(rawQuery.split("&"))
                .filter(parameter -> !parameter.isBlank())
                .filter(parameter -> !isTrackingParameter(parameter))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("&"));

        return normalized.isBlank() ? null : normalized;
    }

    private boolean isTrackingParameter(String parameter) {
        String name = parameter.split("=", 2)[0].toLowerCase(Locale.ROOT);
        return name.startsWith("utm_")
                || name.equals("fbclid")
                || name.equals("gclid")
                || name.equals("msclkid");
    }

    private String buildUrl(String scheme, String userInfo, String host, int port, String path, String query) {
        StringBuilder builder = new StringBuilder();
        if (scheme != null) {
            builder.append(scheme).append(':');
        }
        if (host != null) {
            builder.append("//");
            if (userInfo != null) {
                builder.append(userInfo).append('@');
            }
            builder.append(host);
            if (port >= 0) {
                builder.append(':').append(port);
            }
        }
        builder.append(path);
        if (query != null) {
            builder.append('?').append(query);
        }
        return builder.toString();
    }
}

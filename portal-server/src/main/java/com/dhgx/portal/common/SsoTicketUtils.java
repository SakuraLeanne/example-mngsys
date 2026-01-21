package com.dhgx.portal.common;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * SSO ticket 工具类。
 */
public final class SsoTicketUtils {
    private static final String TICKET_PREFIX = "portal:sso:ticket:";

    private SsoTicketUtils() {
    }

    public static String buildTicketKey(String ticket) {
        return TICKET_PREFIX + ticket;
    }

    public static String hashRedirectUri(String redirectUri) {
        String canonical = canonicalizeRedirectUri(redirectUri);
        if (!StringUtils.hasText(canonical)) {
            return "";
        }
        return sha256Hex(canonical);
    }

    public static String hashValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return sha256Hex(value.trim());
    }

    public static String canonicalizeRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return "";
        }
        URI uri;
        try {
            uri = URI.create(redirectUri.trim());
        } catch (IllegalArgumentException ex) {
            return "";
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String normalizedScheme = scheme == null ? null : scheme.toLowerCase(Locale.ROOT);
        String normalizedHost = host == null ? null : host.toLowerCase(Locale.ROOT);
        String path = uri.getRawPath();
        String normalizedPath = path == null ? "" : path;
        String query = uri.getRawQuery();
        String userInfo = uri.getUserInfo();
        try {
            URI normalized = new URI(
                    normalizedScheme,
                    userInfo,
                    normalizedHost,
                    uri.getPort(),
                    normalizedPath,
                    query,
                    null);
            return normalized.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static String maskTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return "";
        }
        String trimmed = ticket.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("无法生成 redirectUri hash", ex);
        }
    }
}

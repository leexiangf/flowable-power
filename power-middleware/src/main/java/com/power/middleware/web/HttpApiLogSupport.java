package com.power.middleware.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class HttpApiLogSupport {

    private static final Pattern SENSITIVE_JSON =
            Pattern.compile("\"(password|oldPassword|newPassword|accessToken|refreshToken)\"\\s*:\\s*\"[^\"]*\"",
                    Pattern.CASE_INSENSITIVE);

    private HttpApiLogSupport() {
    }

    static boolean shouldLogBody(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }
        String lower = contentType.toLowerCase();
        return lower.contains("json")
                || lower.contains("text")
                || lower.contains("xml")
                || lower.contains("form");
    }

    static String readCachedBody(byte[] content, String contentType, String characterEncoding, int maxLength) {
        if (content == null || content.length == 0) {
            return "";
        }
        Charset charset = resolveCharset(contentType, characterEncoding);
        String raw = new String(content, charset);
        return truncate(maskSensitive(raw), maxLength);
    }

    static String maskSensitive(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return SENSITIVE_JSON.matcher(text).replaceAll("\"$1\":\"***\"");
    }

    static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }

    static Map<String, String> queryParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values == null || values.length == 0) {
                params.put(key, "");
            } else if (values.length == 1) {
                params.put(key, maskParam(key, values[0]));
            } else {
                params.put(key, String.join(",", values));
            }
        });
        return params.isEmpty() ? Collections.emptyMap() : params;
    }

    static String maskParam(String name, String value) {
        if (value == null) {
            return "";
        }
        if (name != null && name.toLowerCase().contains("password")) {
            return "***";
        }
        if ("accessToken".equalsIgnoreCase(name) || "refreshToken".equalsIgnoreCase(name)) {
            return "***";
        }
        return value;
    }

    static Charset resolveCharset(String contentType, String characterEncoding) {
        if (StringUtils.hasText(contentType)) {
            Charset fromHeader = parseCharsetFromContentType(contentType);
            if (fromHeader != null) {
                return fromHeader;
            }
            String lower = contentType.toLowerCase();
            if (lower.contains("json") || lower.contains("text") || lower.contains("xml") || lower.contains("+json")) {
                return StandardCharsets.UTF_8;
            }
        }
        if (!StringUtils.hasText(characterEncoding)
                || isLatin1(characterEncoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(characterEncoding.trim());
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private static Charset parseCharsetFromContentType(String contentType) {
        int idx = contentType.toLowerCase().indexOf("charset=");
        if (idx < 0) {
            return null;
        }
        String charsetName = contentType.substring(idx + "charset=".length()).trim();
        int semi = charsetName.indexOf(';');
        if (semi >= 0) {
            charsetName = charsetName.substring(0, semi).trim();
        }
        if (charsetName.startsWith("\"") && charsetName.endsWith("\"") && charsetName.length() > 1) {
            charsetName = charsetName.substring(1, charsetName.length() - 1);
        }
        if (!StringUtils.hasText(charsetName)) {
            return null;
        }
        try {
            return Charset.forName(charsetName);
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isLatin1(String encoding) {
        String name = encoding.trim().toLowerCase();
        return "iso-8859-1".equals(name) || "iso8859-1".equals(name) || "latin1".equals(name);
    }
}

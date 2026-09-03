package com.power.common.constant;

/**
 * Client login platform. Extend later as needed.
 */
public enum ClientPlatform {
    WEB,
    MOBILE;

    public static ClientPlatform from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("platform is required");
        }
        return ClientPlatform.valueOf(raw.trim().toUpperCase());
    }
}

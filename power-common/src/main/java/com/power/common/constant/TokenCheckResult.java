package com.power.common.constant;

/**
 * Result of Redis session validation for an access token.
 */
public enum TokenCheckResult {
    ALLOWED,
    /** Generic invalid / expired / logout blacklist / version mismatch. */
    DENIED,
    /** Explicitly kicked by login on another (or same) platform. */
    KICKED_BY_OTHER_PLATFORM,
    USER_DISABLED
}

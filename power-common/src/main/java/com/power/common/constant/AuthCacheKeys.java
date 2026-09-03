package com.power.common.constant;

/**
 * Auth-related Redis key conventions (Gateway + services share the same keys).
 */
public final class AuthCacheKeys {

    public static final String ACCESS_BLACKLIST = "power:auth:bl:access:";
    public static final String REFRESH_SESSION = "power:auth:refresh:";
    /** Current session version for a user; JWT claim {@code ver} must be >= this. */
    public static final String USER_TOKEN_VERSION = "power:auth:user:ver:";
    /** Marker that user is disabled; presence rejects all tokens immediately. */
    public static final String USER_DISABLED = "power:auth:user:disabled:";
    /** Online session per user + platform: value = accessJti|refreshJti */
    public static final String USER_ONLINE = "power:auth:online:";
    /** Refresh was revoked due to platform kick (distinct from missing session). */
    public static final String REFRESH_KICKED = "power:auth:kick:refresh:";

    private AuthCacheKeys() {
    }

    public static String accessBlacklist(String jti) {
        return ACCESS_BLACKLIST + jti;
    }

    public static String refreshSession(String jti) {
        return REFRESH_SESSION + jti;
    }

    public static String refreshKicked(String jti) {
        return REFRESH_KICKED + jti;
    }

    public static String userTokenVersion(Long userId) {
        return USER_TOKEN_VERSION + userId;
    }

    public static String userDisabled(Long userId) {
        return USER_DISABLED + userId;
    }

    public static String userOnline(Long userId, ClientPlatform platform) {
        return USER_ONLINE + userId + ":" + platform.name();
    }
}

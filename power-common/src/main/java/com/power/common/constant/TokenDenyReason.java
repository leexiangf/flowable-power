package com.power.common.constant;

/**
 * Access token deny reasons stored as Redis blacklist values.
 */
public final class TokenDenyReason {

    public static final String LOGOUT = "logout";
    /** Kicked because another platform (or same platform new device) logged in. */
    public static final String PLATFORM_KICK = "platform_kick";

    private TokenDenyReason() {
    }

    public static boolean isPlatformKick(String reason) {
        return PLATFORM_KICK.equals(reason);
    }
}

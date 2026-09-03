package com.power.workflow.support;

import org.flowable.common.engine.impl.identity.Authentication;

import java.util.function.Supplier;

public final class FlowableUserContext {

    private FlowableUserContext() {
    }

    public static void runAs(Long userId, Runnable action) {
        String previous = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId(userId == null ? null : String.valueOf(userId));
            action.run();
        } finally {
            Authentication.setAuthenticatedUserId(previous);
        }
    }

    public static <T> T callAs(Long userId, Supplier<T> action) {
        String previous = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId(userId == null ? null : String.valueOf(userId));
            return action.get();
        } finally {
            Authentication.setAuthenticatedUserId(previous);
        }
    }
}

package com.github.catatafishen.ideagentforcopilot.bridge;

import java.util.function.Consumer;

/**
 * A permission request surfaced to the UI when a tool has ASK permission mode.
 * Call {@link #respond} with {@code true} to allow, {@code false} to deny.
 */
public class PermissionRequest {
    public final long reqId;
    public final String toolId;
    public final String displayName;
    public final String description;
    private final Consumer<Boolean> respondFn;

    public PermissionRequest(long reqId, String toolId, String displayName, String description,
                             Consumer<Boolean> respondFn) {
        this.reqId = reqId;
        this.toolId = toolId;
        this.displayName = displayName;
        this.description = description;
        this.respondFn = respondFn;
    }

    public void respond(boolean allowed) {
        respondFn.accept(allowed);
    }
}

package com.github.catatafishen.ideagentforcopilot.bridge;

/**
 * A permission request surfaced to the UI when a tool has ASK permission mode.
 * Call {@link #respond} with the chosen response level.
 */
public class PermissionRequest {
    public final long reqId;
    public final String toolId;
    public final String displayName;
    public final String description;
    private final java.util.function.Consumer<PermissionResponse> respondFn;

    public PermissionRequest(long reqId, String toolId, String displayName, String description,
                             java.util.function.Consumer<PermissionResponse> respondFn) {
        this.reqId = reqId;
        this.toolId = toolId;
        this.displayName = displayName;
        this.description = description;
        this.respondFn = respondFn;
    }

    /**
     * @deprecated Use {@link #respond(PermissionResponse)} instead.
     */
    @Deprecated(forRemoval = true)
    public void respond(boolean allowed) {
        respondFn.accept(allowed ? PermissionResponse.ALLOW_ONCE : PermissionResponse.DENY);
    }

    public void respond(PermissionResponse response) {
        respondFn.accept(response);
    }
}

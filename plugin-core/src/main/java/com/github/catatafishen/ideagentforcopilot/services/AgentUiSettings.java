package com.github.catatafishen.ideagentforcopilot.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-agent UI settings interface.
 * Each agent (Copilot, Claude, etc.) provides its own implementation that delegates
 * to its concrete settings class. The UI layer reads and writes settings through this
 * interface instead of referencing a concrete settings class directly.
 *
 * <p>Shared UI preferences (attach trigger, follow-agent-files) live on
 * {@link ActiveAgentManager} because they are not agent-specific.</p>
 */
public interface AgentUiSettings {

    // ── Model selection ──────────────────────────────────────────────────────

    @Nullable
    String getSelectedModel();

    void setSelectedModel(@NotNull String modelId);

    // ── Session mode ─────────────────────────────────────────────────────────

    @NotNull
    String getSessionMode();

    void setSessionMode(@NotNull String mode);

    // ── Active sub-agent label (runtime-only) ────────────────────────────────

    @Nullable
    String getActiveAgentLabel();

    void setActiveAgentLabel(@Nullable String label);

    // ── Tool permissions ─────────────────────────────────────────────────────

    @NotNull
    ToolPermission getToolPermission(@NotNull String toolId);

    void setToolPermission(@NotNull String toolId, @NotNull ToolPermission perm);

    @NotNull
    ToolPermission getToolPermissionInsideProject(@NotNull String toolId);

    void setToolPermissionInsideProject(@NotNull String toolId, @NotNull ToolPermission perm);

    @NotNull
    ToolPermission getToolPermissionOutsideProject(@NotNull String toolId);

    void setToolPermissionOutsideProject(@NotNull String toolId, @NotNull ToolPermission perm);

    void clearToolSubPermissions(@NotNull String toolId);
}

package com.github.catatafishen.ideagentforcopilot.bridge;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy interface for agent-specific configuration and lifecycle.
 * Each ACP-compatible agent (Copilot CLI, Claude Code, Codex CLI, etc.)
 * provides an implementation that handles binary discovery, authentication,
 * model metadata parsing, and pre-launch setup.
 *
 * <p>The generic {@link AcpClient} delegates all agent-specific concerns
 * to this interface, keeping the JSON-RPC protocol layer agent-agnostic.</p>
 */
public interface AgentConfig {

    /**
     * Human-readable agent name for logs and notifications (e.g., "Copilot", "Claude Code").
     */
    @NotNull
    String getDisplayName();

    /**
     * IntelliJ notification group ID for status notifications.
     */
    @NotNull
    String getNotificationGroupId();

    /**
     * Pre-launch setup (e.g., ensure instruction files exist).
     * Called before the agent process is started.
     */
    void prepareForLaunch(@Nullable String projectBasePath);

    /**
     * Find the agent binary on the system.
     *
     * @return absolute path to the agent binary
     * @throws CopilotException if the binary cannot be found
     */
    @NotNull
    String findAgentBinary() throws CopilotException;

    /**
     * Build the ProcessBuilder for launching the agent in ACP mode.
     *
     * @param binaryPath      path returned by {@link #findAgentBinary()}
     * @param projectBasePath project root (for config-dir, working directory)
     * @return configured ProcessBuilder ready to start
     * @throws CopilotException if the command cannot be built
     */
    @NotNull
    ProcessBuilder buildAcpProcess(@NotNull String binaryPath, @Nullable String projectBasePath)
        throws CopilotException;

    /**
     * Extract agent-specific data from the ACP {@code initialize} response.
     * Called once after the handshake completes.
     */
    void parseInitializeResponse(@NotNull JsonObject result);

    /**
     * Extract a usage/cost multiplier string from model metadata (e.g., "1x", "3x").
     *
     * @param modelMeta the {@code _meta} object from a model entry, or null
     * @return usage string, or null if not available
     */
    @Nullable
    String parseModelUsage(@Nullable JsonObject modelMeta);

    /**
     * Get the authentication method info parsed from the last initialize response.
     *
     * @return auth method, or null if not available or not applicable
     */
    @Nullable
    AcpClient.AuthMethod getAuthMethod();

    /**
     * Get the resolved path to the agent binary (for external commands like login/logout).
     */
    @Nullable
    String getAgentBinaryPath();
}

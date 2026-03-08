package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AcpClient;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for ACP agent lifecycle management.
 * Each concrete subclass provides its own {@link AgentConfig} and {@link AgentSettings}
 * (e.g., Copilot, Claude Code, Codex CLI) and is registered as an IntelliJ project service.
 *
 * <p>Generic concerns handled here: process start/stop, health checks, auto-restart,
 * client access. Agent-specific concerns (binary discovery, auth, model parsing,
 * permission settings) are delegated to the config and settings objects.</p>
 */
public abstract class AgentService implements Disposable {
    private static final Logger LOG = Logger.getInstance(AgentService.class);

    protected final Project project;
    private AcpClient acpClient;
    private AgentConfig cachedConfig;
    private volatile boolean started = false;

    protected AgentService(@NotNull Project project) {
        this.project = project;
        LOG.info(getDisplayName() + " agent service initialised for project: " + project.getName());
    }

    /**
     * Create the agent-specific configuration (binary discovery, auth, model metadata).
     */
    @NotNull
    protected abstract AgentConfig createAgentConfig();

    /**
     * Create the agent-specific settings (timeouts, permissions).
     */
    @NotNull
    protected abstract AgentSettings createAgentSettings();

    /**
     * Provide the per-agent UI settings (model selection, session mode, tool permissions).
     * The UI layer reads and writes settings through this interface.
     */
    @NotNull
    public abstract AgentUiSettings getUiSettings();

    /**
     * Returns the agent configuration. Used by the UI to query agent capabilities
     * (e.g., supported modes via {@link AgentConfig#getSupportedModes()}).
     * Cached to avoid repeated instantiation from toolbar update calls.
     */
    @NotNull
    public AgentConfig getConfig() {
        if (cachedConfig == null) {
            cachedConfig = createAgentConfig();
        }
        return cachedConfig;
    }

    /**
     * Human-readable name for log messages (e.g., "Copilot", "Claude Code").
     */
    @NotNull
    protected abstract String getDisplayName();

    /**
     * Start the ACP process if not already running.
     */
    public synchronized void start() {
        if (started && acpClient != null && acpClient.isHealthy()) {
            LOG.debug("ACP client already running for " + getDisplayName());
            return;
        }

        try {
            LOG.info("Starting " + getDisplayName() + " ACP client for project: " + project.getName());
            if (acpClient != null) {
                acpClient.close();
            }

            String projectPath = project.getBasePath();
            acpClient = new AcpClient(createAgentConfig(), createAgentSettings(), projectPath);
            acpClient.start();
            started = true;
            LOG.info(getDisplayName() + " ACP client started with config-dir: " + projectPath + "/.agent-work");

        } catch (Exception e) {
            LOG.error("Failed to start " + getDisplayName() + " ACP client", e);
            throw new RuntimeException("Failed to start " + getDisplayName() + " ACP client", e);
        }
    }

    /**
     * Get the ACP client for making calls.
     * Starts the client if not already running.
     */
    @NotNull
    public AcpClient getClient() {
        if (!started || acpClient == null || !acpClient.isHealthy()) {
            start();
        }
        return acpClient;
    }

    /**
     * Stop the ACP client.
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            LOG.info("Stopping " + getDisplayName() + " ACP client...");
            if (acpClient != null) {
                acpClient.close();
            }
            started = false;
            acpClient = null;
        } catch (Exception e) {
            LOG.error("Failed to stop " + getDisplayName() + " ACP client", e);
        }
    }

    /**
     * Restart the agent process.
     */
    public synchronized void restart() {
        LOG.info("Restarting " + getDisplayName() + " ACP client");
        stop();
        start();
    }

    @Override
    public void dispose() {
        LOG.info(getDisplayName() + " agent service disposed");
        stop();
    }
}

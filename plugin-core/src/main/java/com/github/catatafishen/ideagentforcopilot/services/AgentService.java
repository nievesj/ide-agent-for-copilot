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
     * If the user has overridden the start command (via the connect panel),
     * the override is used instead of the agent's built-in binary discovery.
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
            int mcpPort = resolveMcpPort();
            AgentConfig config = resolveStartConfig();
            acpClient = new AcpClient(config, createAgentSettings(), projectPath, mcpPort);
            acpClient.start();
            started = true;
            LOG.info(getDisplayName() + " ACP client started with config-dir: " + projectPath + "/.agent-work");

        } catch (Exception e) {
            LOG.error("Failed to start " + getDisplayName() + " ACP client", e);
            throw new RuntimeException("Failed to start " + getDisplayName() + " ACP client", e);
        }
    }

    /**
     * Determines the MCP port to pass to the CLI. Ensures a server is actually
     * running on the returned port, or returns 0 (meaning: skip MCP config).
     */
    private int resolveMcpPort() {
        // 1. Try McpServerControl (McpHttpServer) — the full MCP endpoint
        McpServerControl mcpServer = McpServerControl.getInstance(project);
        if (mcpServer != null) {
            if (mcpServer.isRunning() && mcpServer.getPort() > 0) {
                LOG.info("MCP server already running on port " + mcpServer.getPort());
                return mcpServer.getPort();
            }
            // Auto-start McpHttpServer so the MCP JAR has something to talk to
            try {
                mcpServer.start();
                if (mcpServer.isRunning() && mcpServer.getPort() > 0) {
                    LOG.info("Auto-started MCP server on port " + mcpServer.getPort());
                    return mcpServer.getPort();
                }
            } catch (Exception e) {
                LOG.warn("Failed to auto-start MCP server: " + e.getMessage());
            }
        }

        // 2. No McpHttpServer available — MCP JAR needs /mcp endpoint,
        //    which PsiBridgeService doesn't provide. Skip MCP config.
        LOG.warn("No MCP server available — IntelliJ code tools will be unavailable for this session. "
            + "Start the MCP server from the connection panel to enable tools.");
        return 0;
    }

    /**
     * Returns the config to use for starting the ACP process.
     * If the user edited the start command away from the agent's default,
     * wraps the custom command in a {@link com.github.catatafishen.ideagentforcopilot.bridge.GenericCustomAgentConfig}
     * that delegates non-process concerns (display name, init parsing) to the real config.
     */
    @NotNull
    private AgentConfig resolveStartConfig() {
        ActiveAgentManager mgr = ActiveAgentManager.getInstance(project);
        ActiveAgentManager.AgentType type = mgr.getActiveType();
        String storedCommand = mgr.getCustomAcpCommand();
        String defaultCommand = type.defaultStartCommand();

        // Use the agent-specific config when:
        //  - no stored custom command (shouldn't happen, but be safe)
        //  - the stored command matches the agent's default
        //  - the agent is GENERIC (GenericCustomService already creates the right config)
        if (storedCommand.isEmpty()
            || storedCommand.equals(defaultCommand)
            || type == ActiveAgentManager.AgentType.GENERIC) {
            return createAgentConfig();
        }

        // User has customised the command — use GenericCustomAgentConfig with the real
        // config's display name and init-response parsing so agent features still work.
        LOG.info("Using custom start command for " + getDisplayName() + ": " + storedCommand);
        AgentConfig realConfig = createAgentConfig();
        return new CommandOverrideAgentConfig(realConfig, storedCommand);
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

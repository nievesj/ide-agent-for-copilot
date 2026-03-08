package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AcpClient;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Project service that manages which ACP agent is currently active.
 * The UI layer uses this instead of referencing concrete agent services directly,
 * keeping it agent-agnostic.
 *
 * <p>Also hosts shared UI preferences that apply regardless of which agent is active
 * (attach trigger character, follow-agent-files).</p>
 */
@Service(Service.Level.PROJECT)
public final class ActiveAgentManager {

    private static final Logger LOG = Logger.getInstance(ActiveAgentManager.class);

    private static final String KEY_ACTIVE_AGENT = "agent.activeType";
    private static final String KEY_ATTACH_TRIGGER = "agent.attachTriggerChar";
    private static final String DEFAULT_ATTACH_TRIGGER = "#";
    private static final String KEY_FOLLOW_AGENT_FILES = "agent.followAgentFiles";
    private static final String KEY_AUTO_CONNECT = "agent.autoConnect";
    private static final String KEY_CUSTOM_ACP_COMMAND = "agent.customAcpCommand";
    private static final String KEY_AUTO_APPROVE = "agent.autoApprovePermissions";

    private final Project project;
    private volatile boolean acpConnected;

    public ActiveAgentManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static ActiveAgentManager getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, ActiveAgentManager.class);
    }

    // ── Agent type ───────────────────────────────────────────────────────────

    public enum AgentType {
        COPILOT("copilot", "GitHub Copilot", "copilot-cli --acp --stdio"),
        CLAUDE("claude", "Claude Code", "claude --acp --stdio"),
        KIRO("kiro", "Kiro", "kiro-cli acp"),
        GEMINI("gemini", "Gemini", "gemini --experimental-acp"),
        OPENCODE("opencode", "OpenCode", "opencode acp"),
        CLINE("cline", "Cline", "cline --acp"),
        GENERIC("generic", "Generic ACP", "");

        private final String id;
        private final String displayName;
        private final String defaultStartCommand;

        AgentType(String id, String displayName, String defaultStartCommand) {
            this.id = id;
            this.displayName = displayName;
            this.defaultStartCommand = defaultStartCommand;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        /**
         * Default CLI command for starting this agent in ACP mode.
         * Shown in the connect panel as a prefilled hint the user can edit.
         * Empty for GENERIC (user must supply their own).
         */
        public String defaultStartCommand() {
            return defaultStartCommand;
        }

        @NotNull
        static AgentType fromId(@NotNull String id) {
            for (AgentType t : values()) {
                if (t.id.equals(id)) return t;
            }
            return COPILOT;
        }
    }

    private final java.util.List<Runnable> switchListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Register a callback invoked after the active agent changes.
     * Typically used by the UI to reset sessions and reload models.
     */
    public void addSwitchListener(@NotNull Runnable listener) {
        switchListeners.add(listener);
    }

    public void removeSwitchListener(@NotNull Runnable listener) {
        switchListeners.remove(listener);
    }

    @NotNull
    public AgentType getActiveType() {
        String stored = PropertiesComponent.getInstance(project).getValue(KEY_ACTIVE_AGENT);
        return stored != null ? AgentType.fromId(stored) : AgentType.COPILOT;
    }

    public void switchAgent(@NotNull AgentType type) {
        AgentType previous = getActiveType();
        if (previous == type) return;

        LOG.info("Switching active agent from " + previous.id() + " to " + type.id());
        PropertiesComponent.getInstance(project).setValue(KEY_ACTIVE_AGENT, type.id());

        for (Runnable listener : switchListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                LOG.warn("Agent switch listener failed", e);
            }
        }
    }

    // ── Active service shortcuts ─────────────────────────────────────────────

    @NotNull
    public AgentService getService() {
        return switch (getActiveType()) {
            case CLAUDE -> ClaudeService.getInstance(project);
            case KIRO -> KiroService.getInstance(project);
            case GEMINI -> GeminiService.getInstance(project);
            case OPENCODE -> OpenCodeService.getInstance(project);
            case CLINE -> ClineService.getInstance(project);
            case GENERIC -> GenericCustomService.getInstance(project);
            default -> CopilotService.getInstance(project);
        };
    }

    @NotNull
    public AcpClient getClient() {
        return getService().getClient();
    }

    @NotNull
    public AgentUiSettings getSettings() {
        return getService().getUiSettings();
    }

    // ── Shared UI preferences (agent-agnostic) ──────────────────────────────

    /**
     * Trigger character for file search in chat input.
     * "#" (VS Code Copilot style, default), "@" (JetBrains AI style), or "" (disabled).
     */
    @NotNull
    public static String getAttachTriggerChar() {
        return PropertiesComponent.getInstance().getValue(KEY_ATTACH_TRIGGER, DEFAULT_ATTACH_TRIGGER);
    }

    public static void setAttachTriggerChar(@NotNull String trigger) {
        PropertiesComponent.getInstance().setValue(KEY_ATTACH_TRIGGER, trigger, DEFAULT_ATTACH_TRIGGER);
    }

    /**
     * Whether to open files in the editor when the agent reads/writes them.
     * Project-scoped so each open project can have its own setting.
     */
    public static boolean getFollowAgentFiles(@NotNull Project project) {
        return PropertiesComponent.getInstance(project).getBoolean(KEY_FOLLOW_AGENT_FILES, true);
    }

    public static void setFollowAgentFiles(@NotNull Project project, boolean enabled) {
        PropertiesComponent.getInstance(project).setValue(KEY_FOLLOW_AGENT_FILES, enabled, true);
    }

    // ── ACP connection state ─────────────────────────────────────────────────

    public boolean isAcpConnected() {
        return acpConnected;
    }

    public void setAcpConnected(boolean connected) {
        this.acpConnected = connected;
    }

    // ── Auto-connect on startup ──────────────────────────────────────────────

    public boolean isAutoConnect() {
        return PropertiesComponent.getInstance(project).getBoolean(KEY_AUTO_CONNECT, false);
    }

    public void setAutoConnect(boolean enabled) {
        PropertiesComponent.getInstance(project).setValue(KEY_AUTO_CONNECT, enabled, false);
    }

    // ── Generic ACP custom command ───────────────────────────────────────────

    /**
     * Returns the user-customized ACP start command for the currently active agent.
     * Falls back to the agent type's default if the user hasn't overridden it.
     */
    @NotNull
    public String getCustomAcpCommand() {
        AgentType type = getActiveType();
        String stored = PropertiesComponent.getInstance(project)
            .getValue(KEY_CUSTOM_ACP_COMMAND + "." + type.id());
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        // Legacy fallback: check the old unqualified key (for GENERIC before per-agent storage)
        String legacy = PropertiesComponent.getInstance(project).getValue(KEY_CUSTOM_ACP_COMMAND, "");
        if (!legacy.isEmpty() && type == AgentType.GENERIC) {
            return legacy;
        }
        return type.defaultStartCommand();
    }

    /**
     * Stores a user-customized ACP start command for the currently active agent.
     * If the command matches the agent's default, it's cleared (so future default changes take effect).
     */
    public void setCustomAcpCommand(@NotNull String command) {
        AgentType type = getActiveType();
        String value = command.equals(type.defaultStartCommand()) ? "" : command;
        PropertiesComponent.getInstance(project)
            .setValue(KEY_CUSTOM_ACP_COMMAND + "." + type.id(), value, "");
    }

    /**
     * Returns the user-customized ACP start command for a specific agent type.
     * Falls back to the agent type's default if the user hasn't overridden it.
     */
    @NotNull
    public String getCustomAcpCommandFor(@NotNull AgentType type) {
        String stored = PropertiesComponent.getInstance(project)
            .getValue(KEY_CUSTOM_ACP_COMMAND + "." + type.id());
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        String legacy = PropertiesComponent.getInstance(project).getValue(KEY_CUSTOM_ACP_COMMAND, "");
        if (!legacy.isEmpty() && type == AgentType.GENERIC) {
            return legacy;
        }
        return type.defaultStartCommand();
    }

    /**
     * Stores a user-customized ACP start command for a specific agent type.
     */
    public void setCustomAcpCommandFor(@NotNull AgentType type, @NotNull String command) {
        String value = command.equals(type.defaultStartCommand()) ? "" : command;
        PropertiesComponent.getInstance(project)
            .setValue(KEY_CUSTOM_ACP_COMMAND + "." + type.id(), value, "");
    }

    // ── Auto-approve permissions (plugin-level, applies to all agents) ──────

    /**
     * When enabled, all ASK permission requests are automatically approved (DENY is still respected).
     * This is a plugin-level feature, not an agent-specific mode.
     */
    public boolean isAutoApprovePermissions() {
        return PropertiesComponent.getInstance(project).getBoolean(KEY_AUTO_APPROVE, false);
    }

    public void setAutoApprovePermissions(boolean enabled) {
        PropertiesComponent.getInstance(project).setValue(KEY_AUTO_APPROVE, enabled, false);
    }
}

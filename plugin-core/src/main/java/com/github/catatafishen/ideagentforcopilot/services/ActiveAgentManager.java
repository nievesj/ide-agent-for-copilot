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

    private final Project project;

    public ActiveAgentManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static ActiveAgentManager getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, ActiveAgentManager.class);
    }

    // ── Agent type ───────────────────────────────────────────────────────────

    public enum AgentType {
        COPILOT("copilot"),
        CLAUDE("claude");

        private final String id;

        AgentType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        @NotNull
        static AgentType fromId(@NotNull String id) {
            for (AgentType t : values()) {
                if (t.id.equals(id)) return t;
            }
            return COPILOT;
        }
    }

    @NotNull
    public AgentType getActiveType() {
        String stored = PropertiesComponent.getInstance(project).getValue(KEY_ACTIVE_AGENT);
        return stored != null ? AgentType.fromId(stored) : AgentType.COPILOT;
    }

    public void switchAgent(@NotNull AgentType type) {
        LOG.info("Switching active agent to: " + type.id());
        PropertiesComponent.getInstance(project).setValue(KEY_ACTIVE_AGENT, type.id());
    }

    // ── Active service shortcuts ─────────────────────────────────────────────

    @NotNull
    public AgentService getService() {
        return switch (getActiveType()) {
            case CLAUDE -> ClaudeService.getInstance(project);
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
}

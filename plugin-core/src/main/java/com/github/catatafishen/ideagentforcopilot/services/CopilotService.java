package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.CopilotAgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.CopilotAgentSettings;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copilot-specific agent service.
 * Provides {@link CopilotAgentConfig} and {@link CopilotAgentSettings} to the
 * generic {@link AgentService} lifecycle. Registered as an IntelliJ project service.
 */
@Service(Service.Level.PROJECT)
@SuppressWarnings("java:S112") // RuntimeException wraps startup failures for service initialization
public final class CopilotService extends AgentService {

    public CopilotService(@NotNull Project project) {
        super(project);
    }

    @NotNull
    public static CopilotService getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, CopilotService.class);
    }

    private final AgentUiSettings uiSettings = new AgentUiSettings() {
        @Override
        public @Nullable String getSelectedModel() {
            return CopilotSettings.getSelectedModel();
        }

        @Override
        public void setSelectedModel(@NotNull String modelId) {
            CopilotSettings.setSelectedModel(modelId);
        }

        @Override
        public @NotNull String getSessionMode() {
            return CopilotSettings.getSessionMode();
        }

        @Override
        public void setSessionMode(@NotNull String mode) {
            CopilotSettings.setSessionMode(mode);
        }

        @Override
        public @Nullable String getActiveAgentLabel() {
            return CopilotSettings.getActiveAgentLabel();
        }

        @Override
        public void setActiveAgentLabel(@Nullable String label) {
            CopilotSettings.setActiveAgentLabel(label);
        }

        @Override
        public int getPromptTimeout() {
            return CopilotSettings.getPromptTimeout();
        }

        @Override
        public void setPromptTimeout(int seconds) {
            CopilotSettings.setPromptTimeout(seconds);
        }

        @Override
        public int getMaxToolCallsPerTurn() {
            return CopilotSettings.getMaxToolCallsPerTurn();
        }

        @Override
        public void setMaxToolCallsPerTurn(int count) {
            CopilotSettings.setMaxToolCallsPerTurn(count);
        }

        @Override
        public @NotNull ToolPermission getToolPermission(@NotNull String toolId) {
            return CopilotSettings.getToolPermission(toolId);
        }

        @Override
        public void setToolPermission(@NotNull String toolId, @NotNull ToolPermission perm) {
            CopilotSettings.setToolPermission(toolId, perm);
        }

        @Override
        public @NotNull ToolPermission getToolPermissionInsideProject(@NotNull String toolId) {
            return CopilotSettings.getToolPermissionInsideProject(toolId);
        }

        @Override
        public void setToolPermissionInsideProject(@NotNull String toolId, @NotNull ToolPermission perm) {
            CopilotSettings.setToolPermissionInsideProject(toolId, perm);
        }

        @Override
        public @NotNull ToolPermission getToolPermissionOutsideProject(@NotNull String toolId) {
            return CopilotSettings.getToolPermissionOutsideProject(toolId);
        }

        @Override
        public void setToolPermissionOutsideProject(@NotNull String toolId, @NotNull ToolPermission perm) {
            CopilotSettings.setToolPermissionOutsideProject(toolId, perm);
        }

        @Override
        public void clearToolSubPermissions(@NotNull String toolId) {
            CopilotSettings.clearToolSubPermissions(toolId);
        }
    };

    @Override
    public @NotNull AgentUiSettings getUiSettings() {
        return uiSettings;
    }

    @Override
    protected @NotNull AgentConfig createAgentConfig() {
        return new CopilotAgentConfig();
    }

    @Override
    protected @NotNull AgentSettings createAgentSettings() {
        return new CopilotAgentSettings(project);
    }

    @Override
    protected @NotNull String getDisplayName() {
        return "Copilot";
    }

    /**
     * Restart the CLI process so it picks up the new model.
     * <p>
     * Note: Model switching now uses session/set_model (no CLI restart needed).
     * This method is kept as a fallback for edge cases.
     */
    public synchronized void restartWithModel(@NotNull String modelId) {
        restart();
    }
}

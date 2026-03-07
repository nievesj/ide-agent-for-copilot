package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.ClaudeAgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.ClaudeAgentSettings;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Claude Code agent service.
 * Provides {@link ClaudeAgentConfig} and {@link ClaudeAgentSettings} to the
 * generic {@link AgentService} lifecycle. Registered as an IntelliJ project service.
 */
@Service(Service.Level.PROJECT)
public final class ClaudeService extends AgentService {

    public ClaudeService(@NotNull Project project) {
        super(project);
    }

    @NotNull
    public static ClaudeService getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, ClaudeService.class);
    }

    private final AgentUiSettings uiSettings = new AgentUiSettings() {
        @Override
        public @Nullable String getSelectedModel() {
            return ClaudeSettings.getSelectedModel();
        }

        @Override
        public void setSelectedModel(@NotNull String modelId) {
            ClaudeSettings.setSelectedModel(modelId);
        }

        @Override
        public @NotNull String getSessionMode() {
            return ClaudeSettings.getSessionMode();
        }

        @Override
        public void setSessionMode(@NotNull String mode) {
            ClaudeSettings.setSessionMode(mode);
        }

        @Override
        public @Nullable String getActiveAgentLabel() {
            return ClaudeSettings.getActiveAgentLabel();
        }

        @Override
        public void setActiveAgentLabel(@Nullable String label) {
            ClaudeSettings.setActiveAgentLabel(label);
        }

        @Override
        public @NotNull ToolPermission getToolPermission(@NotNull String toolId) {
            return ClaudeSettings.getToolPermission(toolId);
        }

        @Override
        public void setToolPermission(@NotNull String toolId, @NotNull ToolPermission perm) {
            ClaudeSettings.setToolPermission(toolId, perm);
        }

        @Override
        public @NotNull ToolPermission getToolPermissionInsideProject(@NotNull String toolId) {
            return ClaudeSettings.getToolPermissionInsideProject(toolId);
        }

        @Override
        public void setToolPermissionInsideProject(@NotNull String toolId, @NotNull ToolPermission perm) {
            ClaudeSettings.setToolPermissionInsideProject(toolId, perm);
        }

        @Override
        public @NotNull ToolPermission getToolPermissionOutsideProject(@NotNull String toolId) {
            return ClaudeSettings.getToolPermissionOutsideProject(toolId);
        }

        @Override
        public void setToolPermissionOutsideProject(@NotNull String toolId, @NotNull ToolPermission perm) {
            ClaudeSettings.setToolPermissionOutsideProject(toolId, perm);
        }

        @Override
        public void clearToolSubPermissions(@NotNull String toolId) {
            ClaudeSettings.clearToolSubPermissions(toolId);
        }
    };

    @Override
    public @NotNull AgentUiSettings getUiSettings() {
        return uiSettings;
    }

    @Override
    protected @NotNull AgentConfig createAgentConfig() {
        return new ClaudeAgentConfig();
    }

    @Override
    protected @NotNull AgentSettings createAgentSettings() {
        return new ClaudeAgentSettings();
    }

    @Override
    protected @NotNull String getDisplayName() {
        return "Claude";
    }
}

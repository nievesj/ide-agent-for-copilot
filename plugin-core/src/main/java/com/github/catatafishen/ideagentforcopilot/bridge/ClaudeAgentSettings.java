package com.github.catatafishen.ideagentforcopilot.bridge;

import com.github.catatafishen.ideagentforcopilot.services.ActiveAgentManager;
import com.github.catatafishen.ideagentforcopilot.services.ClaudeSettings;
import com.github.catatafishen.ideagentforcopilot.services.ToolPermission;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClaudeAgentSettings implements AgentSettings {

    private final Project project;

    public ClaudeAgentSettings(@Nullable Project project) {
        this.project = project;
    }

    @Override
    public boolean isAutoApprovePermissions() {
        return project != null && ActiveAgentManager.getInstance(project).isAutoApprovePermissions();
    }

    @Override
    public int getPromptTimeout() {
        return ClaudeSettings.getPromptTimeout();
    }

    @Override
    public int getMaxToolCallsPerTurn() {
        return ClaudeSettings.getMaxToolCallsPerTurn();
    }

    @Override
    public @NotNull ToolPermission resolveEffectivePermission(@NotNull String toolId, boolean insideProject) {
        return ClaudeSettings.resolveEffectivePermission(toolId, insideProject);
    }

    @Override
    public @NotNull ToolPermission getToolPermission(@NotNull String toolId) {
        return ClaudeSettings.getToolPermission(toolId);
    }
}

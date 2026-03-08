package com.github.catatafishen.ideagentforcopilot.bridge;

import com.github.catatafishen.ideagentforcopilot.services.ActiveAgentManager;
import com.github.catatafishen.ideagentforcopilot.services.CopilotSettings;
import com.github.catatafishen.ideagentforcopilot.services.ToolPermission;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopilotAgentSettings implements AgentSettings {

    private final Project project;

    public CopilotAgentSettings(@Nullable Project project) {
        this.project = project;
    }

    @Override
    public boolean isAutoApprovePermissions() {
        return project != null && ActiveAgentManager.getInstance(project).isAutoApprovePermissions();
    }

    @Override
    public int getPromptTimeout() {
        return CopilotSettings.getPromptTimeout();
    }

    @Override
    public int getMaxToolCallsPerTurn() {
        return CopilotSettings.getMaxToolCallsPerTurn();
    }

    @Override
    public @NotNull ToolPermission resolveEffectivePermission(@NotNull String toolId, boolean insideProject) {
        return CopilotSettings.resolveEffectivePermission(toolId, insideProject);
    }

    @Override
    public @NotNull ToolPermission getToolPermission(@NotNull String toolId) {
        return CopilotSettings.getToolPermission(toolId);
    }
}

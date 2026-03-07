package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.CopilotAgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.CopilotAgentSettings;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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

    @Override
    protected @NotNull AgentConfig createAgentConfig() {
        return new CopilotAgentConfig();
    }

    @Override
    protected @NotNull AgentSettings createAgentSettings() {
        return new CopilotAgentSettings();
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

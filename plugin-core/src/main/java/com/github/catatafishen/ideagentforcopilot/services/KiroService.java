package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.GenericAgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.KiroAgentConfig;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Kiro CLI agent service.
 * Uses shared {@link GenericSettings} and {@link GenericAgentSettings} to avoid boilerplate.
 */
@Service(Service.Level.PROJECT)
public final class KiroService extends AgentService {

    private static final GenericSettings SETTINGS = new GenericSettings("kiro");
    private final AgentUiSettings uiSettings = new GenericAgentUiSettings(SETTINGS);

    public KiroService(@NotNull Project project) {
        super(project);
    }

    @NotNull
    public static KiroService getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, KiroService.class);
    }

    public static GenericSettings settings() {
        return SETTINGS;
    }

    @Override
    public @NotNull AgentUiSettings getUiSettings() {
        return uiSettings;
    }

    @Override
    protected @NotNull AgentConfig createAgentConfig() {
        return new KiroAgentConfig(SETTINGS);
    }

    @Override
    protected @NotNull AgentSettings createAgentSettings() {
        return new GenericAgentSettings(SETTINGS, project);
    }

    @Override
    protected @NotNull String getDisplayName() {
        return "Kiro";
    }
}

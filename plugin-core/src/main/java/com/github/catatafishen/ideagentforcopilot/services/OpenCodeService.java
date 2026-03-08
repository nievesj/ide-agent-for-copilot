package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.GenericAgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.OpenCodeAgentConfig;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * OpenCode CLI agent service.
 * Uses shared {@link GenericSettings} and {@link GenericAgentSettings} to avoid boilerplate.
 */
@Service(Service.Level.PROJECT)
public final class OpenCodeService extends AgentService {

    private static final GenericSettings SETTINGS = new GenericSettings("opencode");
    private final AgentUiSettings uiSettings = new GenericAgentUiSettings(SETTINGS);

    public OpenCodeService(@NotNull Project project) {
        super(project);
    }

    @NotNull
    public static OpenCodeService getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, OpenCodeService.class);
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
        return new OpenCodeAgentConfig(SETTINGS);
    }

    @Override
    protected @NotNull AgentSettings createAgentSettings() {
        return new GenericAgentSettings(SETTINGS, project);
    }

    @Override
    protected @NotNull String getDisplayName() {
        return "OpenCode";
    }
}

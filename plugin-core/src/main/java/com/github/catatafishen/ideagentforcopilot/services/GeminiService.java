package com.github.catatafishen.ideagentforcopilot.services;

import com.github.catatafishen.ideagentforcopilot.bridge.AgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.AgentSettings;
import com.github.catatafishen.ideagentforcopilot.bridge.GeminiAgentConfig;
import com.github.catatafishen.ideagentforcopilot.bridge.GenericAgentSettings;
import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Google Gemini CLI agent service.
 * Uses shared {@link GenericSettings} and {@link GenericAgentSettings} to avoid boilerplate.
 */
@Service(Service.Level.PROJECT)
public final class GeminiService extends AgentService {

    private static final GenericSettings SETTINGS = new GenericSettings("gemini");
    private final AgentUiSettings uiSettings = new GenericAgentUiSettings(SETTINGS);

    public GeminiService(@NotNull Project project) {
        super(project);
    }

    @NotNull
    public static GeminiService getInstance(@NotNull Project project) {
        return PlatformApiCompat.getService(project, GeminiService.class);
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
        return new GeminiAgentConfig(SETTINGS);
    }

    @Override
    protected @NotNull AgentSettings createAgentSettings() {
        return new GenericAgentSettings(SETTINGS, project);
    }

    @Override
    protected @NotNull String getDisplayName() {
        return "Gemini";
    }
}

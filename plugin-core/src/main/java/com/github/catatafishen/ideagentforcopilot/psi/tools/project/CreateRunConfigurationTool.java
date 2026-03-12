package com.github.catatafishen.ideagentforcopilot.psi.tools.project;

import com.github.catatafishen.ideagentforcopilot.psi.ProjectTools;
import com.github.catatafishen.ideagentforcopilot.psi.RunConfigurationService;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a new run configuration (application, JUnit, or Gradle).
 */
public final class CreateRunConfigurationTool extends ProjectTool {

    private final RunConfigurationService runConfigService;

    public CreateRunConfigurationTool(Project project, ProjectTools projectTools,
                                      RunConfigurationService runConfigService) {
        super(project, projectTools);
        this.runConfigService = runConfigService;
    }

    @Override public @NotNull String id() { return "create_run_configuration"; }
    @Override public @NotNull String displayName() { return "Create Run Config"; }
    @Override public @NotNull String description() { return "Create a new run configuration (application, JUnit, or Gradle)"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return runConfigService.createRunConfiguration(args);
    }
}

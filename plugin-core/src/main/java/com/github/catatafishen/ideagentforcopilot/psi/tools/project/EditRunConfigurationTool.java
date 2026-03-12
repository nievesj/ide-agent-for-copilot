package com.github.catatafishen.ideagentforcopilot.psi.tools.project;

import com.github.catatafishen.ideagentforcopilot.psi.ProjectTools;
import com.github.catatafishen.ideagentforcopilot.psi.RunConfigurationService;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Edits an existing run configuration's arguments, environment, or working directory.
 */
public final class EditRunConfigurationTool extends ProjectTool {

    private final RunConfigurationService runConfigService;

    public EditRunConfigurationTool(Project project, ProjectTools projectTools,
                                    RunConfigurationService runConfigService) {
        super(project, projectTools);
        this.runConfigService = runConfigService;
    }

    @Override public @NotNull String id() { return "edit_run_configuration"; }
    @Override public @NotNull String displayName() { return "Edit Run Config"; }
    @Override public @NotNull String description() { return "Edit an existing run configuration's arguments, environment, or working directory"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return runConfigService.editRunConfiguration(args);
    }
}

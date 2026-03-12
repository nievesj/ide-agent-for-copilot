package com.github.catatafishen.ideagentforcopilot.psi.tools.quality;

import com.github.catatafishen.ideagentforcopilot.psi.CodeQualityTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs IntelliJ's full inspection engine on the project or a specific scope.
 */
@SuppressWarnings("java:S112")
public final class RunInspectionsTool extends QualityTool {

    public RunInspectionsTool(Project project, CodeQualityTools qualityTools) {
        super(project, qualityTools);
    }

    @Override public @NotNull String id() { return "run_inspections"; }
    @Override public @NotNull String displayName() { return "Run Inspections"; }
    @Override public @NotNull String description() { return "Run IntelliJ's full inspection engine on the project or a specific scope"; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return qualityTools.runInspections(args);
    }
}

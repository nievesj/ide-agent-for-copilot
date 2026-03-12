package com.github.catatafishen.ideagentforcopilot.psi.tools.quality;

import com.github.catatafishen.ideagentforcopilot.psi.CodeQualityTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inserts a suppress annotation or comment for a specific inspection at a given line.
 */
@SuppressWarnings("java:S112")
public final class SuppressInspectionTool extends QualityTool {

    public SuppressInspectionTool(Project project, CodeQualityTools qualityTools) {
        super(project, qualityTools);
    }

    @Override public @NotNull String id() { return "suppress_inspection"; }
    @Override public @NotNull String displayName() { return "Suppress Inspection"; }
    @Override public @NotNull String description() { return "Insert a suppress annotation or comment for a specific inspection at a given line"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return qualityTools.suppressInspection(args);
    }
}

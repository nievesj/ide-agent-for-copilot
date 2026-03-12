package com.github.catatafishen.ideagentforcopilot.psi.tools.quality;

import com.github.catatafishen.ideagentforcopilot.psi.CodeQualityTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manually removes unused imports and organizes them according to code style.
 */
@SuppressWarnings("java:S112")
public final class OptimizeImportsTool extends QualityTool {

    public OptimizeImportsTool(Project project, CodeQualityTools qualityTools) {
        super(project, qualityTools);
    }

    @Override public @NotNull String id() { return "optimize_imports"; }
    @Override public @NotNull String displayName() { return "Optimize Imports"; }
    @Override public @NotNull String description() { return "Manually remove unused imports and organize them according to code style"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return qualityTools.optimizeImports(args);
    }
}

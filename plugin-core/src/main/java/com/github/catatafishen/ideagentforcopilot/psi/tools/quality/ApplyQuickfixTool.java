package com.github.catatafishen.ideagentforcopilot.psi.tools.quality;

import com.github.catatafishen.ideagentforcopilot.psi.CodeQualityTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies an IntelliJ quick-fix at a specific file and line.
 */
@SuppressWarnings("java:S112")
public final class ApplyQuickfixTool extends QualityTool {

    public ApplyQuickfixTool(Project project, CodeQualityTools qualityTools) {
        super(project, qualityTools);
    }

    @Override public @NotNull String id() { return "apply_quickfix"; }
    @Override public @NotNull String displayName() { return "Apply Quickfix"; }
    @Override public @NotNull String description() { return "Apply an IntelliJ quick-fix at a specific file and line"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return qualityTools.applyQuickfix(args);
    }
}

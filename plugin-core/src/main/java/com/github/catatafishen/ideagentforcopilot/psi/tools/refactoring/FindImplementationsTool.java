package com.github.catatafishen.ideagentforcopilot.psi.tools.refactoring;

import com.github.catatafishen.ideagentforcopilot.psi.RefactoringTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds all implementations of a class/interface or overrides of a method.
 */
@SuppressWarnings("java:S112")
public final class FindImplementationsTool extends RefactoringTool {

    public FindImplementationsTool(Project project, RefactoringTools refactoringTools) {
        super(project, refactoringTools);
    }

    @Override
    public @NotNull String id() {
        return "find_implementations";
    }

    @Override
    public @NotNull String displayName() {
        return "Find Implementations";
    }

    @Override
    public @NotNull String description() {
        return "Find all implementations of a class/interface or overrides of a method";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return refactoringTools.findImplementationsWrapper(args);
    }
}

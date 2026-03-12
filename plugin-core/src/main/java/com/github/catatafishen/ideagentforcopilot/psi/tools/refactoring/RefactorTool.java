package com.github.catatafishen.ideagentforcopilot.psi.tools.refactoring;

import com.github.catatafishen.ideagentforcopilot.psi.RefactoringTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renames, extracts method, inlines, or safe-deletes a symbol using IntelliJ's refactoring engine.
 */
@SuppressWarnings("java:S112")
public final class RefactorTool extends RefactoringTool {

    public RefactorTool(Project project, RefactoringTools refactoringTools) {
        super(project, refactoringTools);
    }

    @Override
    public @NotNull String id() {
        return "refactor";
    }

    @Override
    public @NotNull String displayName() {
        return "Refactor";
    }

    @Override
    public @NotNull String description() {
        return "Rename, extract method, inline, or safe-delete a symbol using IntelliJ's refactoring engine";
    }

    @Override
    public @NotNull String permissionTemplate() {
        return "{operation} {symbol}";
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return refactoringTools.refactor(args);
    }
}

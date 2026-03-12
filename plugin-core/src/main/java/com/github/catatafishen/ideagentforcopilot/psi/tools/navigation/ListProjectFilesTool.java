package com.github.catatafishen.ideagentforcopilot.psi.tools.navigation;

import com.github.catatafishen.ideagentforcopilot.psi.CodeNavigationTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists files in a project directory, optionally filtered by glob pattern.
 */
@SuppressWarnings("java:S112")
public final class ListProjectFilesTool extends NavigationTool {

    public ListProjectFilesTool(Project project, CodeNavigationTools navTools) {
        super(project, navTools);
    }

    @Override
    public @NotNull String id() {
        return "list_project_files";
    }

    @Override
    public @NotNull String displayName() {
        return "List Project Files";
    }

    @Override
    public @NotNull String description() {
        return "List files in a project directory, optionally filtered by glob pattern";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return navTools.listProjectFiles(args);
    }
}

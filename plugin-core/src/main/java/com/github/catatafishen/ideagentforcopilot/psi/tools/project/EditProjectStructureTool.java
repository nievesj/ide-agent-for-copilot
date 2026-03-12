package com.github.catatafishen.ideagentforcopilot.psi.tools.project;

import com.github.catatafishen.ideagentforcopilot.psi.ProjectTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Views and modifies module dependencies, libraries, SDKs, and project structure.
 */
@SuppressWarnings("java:S112")
public final class EditProjectStructureTool extends ProjectTool {

    public EditProjectStructureTool(Project project, ProjectTools projectTools) {
        super(project, projectTools);
    }

    @Override public @NotNull String id() { return "edit_project_structure"; }
    @Override public @NotNull String displayName() { return "Edit Project Structure"; }
    @Override public @NotNull String description() { return "View and modify module dependencies, libraries, SDKs, and project structure"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return projectTools.editProjectStructure(args);
    }
}

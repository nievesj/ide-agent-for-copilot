package com.github.catatafishen.ideagentforcopilot.psi.tools.editor;

import com.github.catatafishen.ideagentforcopilot.psi.EditorTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens a file in the editor, optionally navigating to a specific line.
 */
@SuppressWarnings("java:S112")
public final class OpenInEditorTool extends EditorTool {

    public OpenInEditorTool(Project project, EditorTools editorTools) {
        super(project, editorTools);
    }

    @Override public @NotNull String id() { return "open_in_editor"; }
    @Override public @NotNull String displayName() { return "Open in Editor"; }
    @Override public @NotNull String description() { return "Open a file in the editor, optionally navigating to a specific line"; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return editorTools.openInEditor(args);
    }
}

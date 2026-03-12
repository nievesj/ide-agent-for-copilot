package com.github.catatafishen.ideagentforcopilot.psi.tools.editor;

import com.github.catatafishen.ideagentforcopilot.psi.EditorTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs a scratch file using an appropriate run configuration.
 */
@SuppressWarnings("java:S112")
public final class RunScratchFileTool extends EditorTool {

    public RunScratchFileTool(Project project, EditorTools editorTools) {
        super(project, editorTools);
    }

    @Override public @NotNull String id() { return "run_scratch_file"; }
    @Override public @NotNull String displayName() { return "Run Scratch File"; }
    @Override public @NotNull String description() { return "Run a scratch file using an appropriate run configuration"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return editorTools.runScratchFile(args);
    }
}

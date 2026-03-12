package com.github.catatafishen.ideagentforcopilot.psi.tools.file;

import com.github.catatafishen.ideagentforcopilot.psi.FileTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Backward-compatible alias for {@link ReadFileTool} using the {@code read_file} id.
 */
@SuppressWarnings("java:S112")
public final class ReadFileAliasTool extends FileTool {

    public ReadFileAliasTool(Project project, FileTools fileTools) {
        super(project, fileTools);
    }

    @Override
    public @NotNull String id() {
        return "read_file";
    }

    @Override
    public @NotNull String displayName() {
        return "Read File";
    }

    @Override
    public @NotNull String description() {
        return "Read a file via IntelliJ's editor buffer -- always returns the current in-memory content";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return fileTools.readFile(args);
    }
}

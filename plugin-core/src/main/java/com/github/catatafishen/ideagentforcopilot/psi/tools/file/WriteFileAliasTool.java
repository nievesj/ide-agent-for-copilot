package com.github.catatafishen.ideagentforcopilot.psi.tools.file;

import com.github.catatafishen.ideagentforcopilot.psi.FileTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Backward-compatible alias for {@link WriteFileTool} using the {@code write_file} id.
 */
@SuppressWarnings("java:S112")
public final class WriteFileAliasTool extends FileTool {

    public WriteFileAliasTool(Project project, FileTools fileTools) {
        super(project, fileTools);
    }

    @Override
    public @NotNull String id() {
        return "write_file";
    }

    @Override
    public @NotNull String displayName() {
        return "Write File";
    }

    @Override
    public @NotNull String description() {
        return "Write full file content or create a new file through IntelliJ's editor buffer. "
                + "Auto-format and import optimization is deferred until turn end "
                + "(controlled by auto_format_and_optimize_imports param)";
    }

    @Override
    public @NotNull String permissionTemplate() {
        return "Write {path}";
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return fileTools.writeFile(args);
    }
}

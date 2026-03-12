package com.github.catatafishen.ideagentforcopilot.psi.tools.file;

import com.github.catatafishen.ideagentforcopilot.psi.FileTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory producing all individual file tool instances.
 * Called from {@link com.github.catatafishen.ideagentforcopilot.psi.PsiBridgeService}
 * during initialization.
 */
public final class FileToolFactory {

    private FileToolFactory() {
    }

    public static @NotNull List<Tool> create(@NotNull Project project, @NotNull FileTools fileTools) {
        return List.of(
                new ReadFileTool(project, fileTools),
                new ReadFileAliasTool(project, fileTools),
                new WriteFileTool(project, fileTools),
                new WriteFileAliasTool(project, fileTools),
                new EditTextTool(project, fileTools),
                new CreateFileTool(project, fileTools),
                new DeleteFileTool(project, fileTools),
                new RenameFileTool(project, fileTools),
                new MoveFileTool(project, fileTools),
                new UndoTool(project, fileTools),
                new RedoTool(project, fileTools),
                new ReloadFromDiskTool(project, fileTools)
        );
    }
}

package com.github.catatafishen.ideagentforcopilot.psi.tools.file;

import com.github.catatafishen.ideagentforcopilot.psi.FileTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.github.catatafishen.ideagentforcopilot.psi.tools.ToolCategory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for file tools. Provides access to the shared
 * {@link FileTools} handler for file operations.
 */
public abstract class FileTool extends Tool {

    protected final FileTools fileTools;

    protected FileTool(Project project, FileTools fileTools) {
        super(project);
        this.fileTools = fileTools;
    }

    @Override
    public @NotNull ToolCategory toolCategory() {
        return ToolCategory.FILE;
    }
}

package com.github.catatafishen.ideagentforcopilot.psi.tools.infrastructure;

import com.github.catatafishen.ideagentforcopilot.psi.InfrastructureTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.github.catatafishen.ideagentforcopilot.psi.tools.ToolCategory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for infrastructure tools. Provides access to the shared
 * {@link InfrastructureTools} for infrastructure operations.
 */
public abstract class InfrastructureTool extends Tool {

    protected final InfrastructureTools infraTools;

    protected InfrastructureTool(Project project, InfrastructureTools infraTools) {
        super(project);
        this.infraTools = infraTools;
    }

    @Override
    public @NotNull ToolCategory toolCategory() {
        return ToolCategory.INFRASTRUCTURE;
    }
}

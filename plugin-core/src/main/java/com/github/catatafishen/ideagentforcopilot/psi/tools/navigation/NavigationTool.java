package com.github.catatafishen.ideagentforcopilot.psi.tools.navigation;

import com.github.catatafishen.ideagentforcopilot.psi.CodeNavigationTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.github.catatafishen.ideagentforcopilot.psi.tools.ToolCategory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for code navigation tools. Provides access to the shared
 * {@link CodeNavigationTools} for symbol search and code exploration.
 */
public abstract class NavigationTool extends Tool {

    protected final CodeNavigationTools navTools;

    protected NavigationTool(Project project, CodeNavigationTools navTools) {
        super(project);
        this.navTools = navTools;
    }

    @Override
    public @NotNull ToolCategory toolCategory() {
        return ToolCategory.NAVIGATION;
    }
}

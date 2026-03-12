package com.github.catatafishen.ideagentforcopilot.psi.tools.navigation;

import com.github.catatafishen.ideagentforcopilot.psi.CodeNavigationTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory producing all individual code navigation tool instances.
 * Called from {@link com.github.catatafishen.ideagentforcopilot.psi.PsiBridgeService}
 * during initialization.
 */
public final class NavigationToolFactory {

    private NavigationToolFactory() {
    }

    public static @NotNull List<Tool> create(
            @NotNull Project project,
            @NotNull CodeNavigationTools navTools,
            boolean hasJava) {
        var tools = new ArrayList<Tool>();
        tools.add(new SearchSymbolsTool(project, navTools));
        tools.add(new GetFileOutlineTool(project, navTools));
        if (hasJava) {
            tools.add(new GetClassOutlineTool(project, navTools));
        }
        tools.add(new FindReferencesTool(project, navTools));
        tools.add(new ListProjectFilesTool(project, navTools));
        tools.add(new SearchTextTool(project, navTools));
        return List.copyOf(tools);
    }
}

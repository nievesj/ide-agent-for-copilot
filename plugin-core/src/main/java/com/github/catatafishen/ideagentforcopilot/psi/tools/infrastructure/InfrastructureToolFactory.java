package com.github.catatafishen.ideagentforcopilot.psi.tools.infrastructure;

import com.github.catatafishen.ideagentforcopilot.psi.InfrastructureTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory that creates all infrastructure tool instances.
 */
public final class InfrastructureToolFactory {

    private InfrastructureToolFactory() {
    }

    public static @NotNull List<Tool> create(@NotNull Project project, @NotNull InfrastructureTools infraTools) {
        return List.of(
                new HttpRequestTool(project, infraTools),
                new RunCommandTool(project, infraTools),
                new ReadIdeLogTool(project, infraTools),
                new GetNotificationsTool(project, infraTools),
                new ReadRunOutputTool(project, infraTools),
                new ReadBuildOutputTool(project, infraTools)
        );
    }
}

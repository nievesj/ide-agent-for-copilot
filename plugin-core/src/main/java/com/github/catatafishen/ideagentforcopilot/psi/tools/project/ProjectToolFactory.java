package com.github.catatafishen.ideagentforcopilot.psi.tools.project;

import com.github.catatafishen.ideagentforcopilot.psi.ProjectTools;
import com.github.catatafishen.ideagentforcopilot.psi.RunConfigurationService;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory producing all individual project tool instances.
 * Conditionally includes Java-specific tools based on plugin availability.
 */
public final class ProjectToolFactory {

    private ProjectToolFactory() {
    }

    public static @NotNull List<Tool> create(
        @NotNull Project project,
        @NotNull ProjectTools projectTools,
        @NotNull RunConfigurationService runConfigService,
        boolean hasJava) {
        var tools = new ArrayList<Tool>();
        tools.add(new GetProjectInfoTool(project, projectTools));
        if (hasJava) {
            tools.add(new BuildProjectTool(project, projectTools));
        }
        tools.add(new GetIndexingStatusTool(project, projectTools));
        tools.add(new DownloadSourcesTool(project, projectTools));
        tools.add(new MarkDirectoryTool(project, projectTools));
        if (hasJava) {
            tools.add(new EditProjectStructureTool(project, projectTools));
        }
        tools.add(new ListRunConfigurationsTool(project, projectTools, runConfigService));
        tools.add(new RunConfigurationTool(project, projectTools, runConfigService));
        tools.add(new CreateRunConfigurationTool(project, projectTools, runConfigService));
        tools.add(new EditRunConfigurationTool(project, projectTools, runConfigService));
        tools.add(new DeleteRunConfigurationTool(project, projectTools, runConfigService));
        return List.copyOf(tools);
    }
}

package com.github.catatafishen.ideagentforcopilot.psi.tools.quality;

import com.github.catatafishen.ideagentforcopilot.psi.CodeQualityTools;
import com.github.catatafishen.ideagentforcopilot.psi.QodanaAnalyzer;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory producing all individual code quality tool instances.
 * Conditionally includes Qodana and SonarQube tools based on plugin availability.
 */
public final class QualityToolFactory {

    private QualityToolFactory() {
    }

    public static @NotNull List<Tool> create(
            @NotNull Project project,
            @NotNull CodeQualityTools qualityTools,
            boolean hasSonar) {
        var tools = new ArrayList<Tool>();
        tools.add(new GetProblemsTool(project, qualityTools));
        tools.add(new GetHighlightsTool(project, qualityTools));
        tools.add(new RunInspectionsTool(project, qualityTools));
        tools.add(new ApplyQuickfixTool(project, qualityTools));
        tools.add(new SuppressInspectionTool(project, qualityTools));
        tools.add(new OptimizeImportsTool(project, qualityTools));
        tools.add(new FormatCodeTool(project, qualityTools));
        tools.add(new AddToDictionaryTool(project, qualityTools));
        tools.add(new GetCompilationErrorsTool(project, qualityTools));

        QodanaAnalyzer qodana = qualityTools.getQodanaAnalyzer();
        if (qodana != null) {
            tools.add(new RunQodanaTool(project, qualityTools, qodana));
        }

        if (hasSonar) {
            tools.add(new RunSonarQubeAnalysisTool(project, qualityTools));
        }

        return List.copyOf(tools);
    }
}

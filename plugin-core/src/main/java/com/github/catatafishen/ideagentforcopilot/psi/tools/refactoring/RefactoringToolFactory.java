package com.github.catatafishen.ideagentforcopilot.psi.tools.refactoring;

import com.github.catatafishen.ideagentforcopilot.psi.RefactoringTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory producing all individual refactoring tool instances.
 * Called from {@link com.github.catatafishen.ideagentforcopilot.psi.PsiBridgeService}
 * during initialization.
 */
public final class RefactoringToolFactory {

    private RefactoringToolFactory() {
    }

    public static @NotNull List<Tool> create(
            @NotNull Project project,
            @NotNull RefactoringTools refactoringTools,
            boolean hasJava) {
        var tools = new ArrayList<Tool>();
        tools.add(new RefactorTool(project, refactoringTools));
        tools.add(new GoToDeclarationTool(project, refactoringTools));
        if (hasJava) {
            tools.add(new GetTypeHierarchyTool(project, refactoringTools));
            tools.add(new FindImplementationsTool(project, refactoringTools));
            tools.add(new GetCallHierarchyTool(project, refactoringTools));
        }
        tools.add(new GetDocumentationTool(project, refactoringTools));
        return List.copyOf(tools);
    }
}

package com.github.catatafishen.ideagentforcopilot.psi.tools.testing;

import com.github.catatafishen.ideagentforcopilot.psi.TestTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory producing all individual testing tool instances.
 */
public final class TestingToolFactory {

    private TestingToolFactory() {
    }

    public static @NotNull List<Tool> create(@NotNull Project project, @NotNull TestTools testTools) {
        return List.of(
                new ListTestsTool(project, testTools),
                new RunTestsTool(project, testTools),
                new GetCoverageTool(project, testTools)
        );
    }
}

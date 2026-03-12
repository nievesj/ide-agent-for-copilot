package com.github.catatafishen.ideagentforcopilot.psi.tools.terminal;

import com.github.catatafishen.ideagentforcopilot.psi.TerminalTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory that creates all terminal tool instances.
 */
public final class TerminalToolFactory {

    private TerminalToolFactory() {
    }

    public static @NotNull List<Tool> create(@NotNull Project project, @NotNull TerminalTools terminalTools) {
        return List.of(
                new RunInTerminalTool(project, terminalTools),
                new WriteTerminalInputTool(project, terminalTools),
                new ReadTerminalOutputTool(project, terminalTools),
                new ListTerminalsTool(project, terminalTools)
        );
    }
}

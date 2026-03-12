package com.github.catatafishen.ideagentforcopilot.psi.tools.terminal;

import com.github.catatafishen.ideagentforcopilot.psi.TerminalTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs a command in IntelliJ's integrated terminal.
 */
@SuppressWarnings("java:S112")
public final class RunInTerminalTool extends TerminalTool {

    public RunInTerminalTool(Project project, TerminalTools terminalTools) {
        super(project, terminalTools);
    }

    @Override public @NotNull String id() { return "run_in_terminal"; }
    @Override public @NotNull String displayName() { return "Run in Terminal"; }
    @Override public @NotNull String description() { return "Run a command in IntelliJ's integrated terminal"; }
    @Override public boolean isOpenWorld() { return true; }
    @Override public @NotNull String permissionTemplate() { return "Run in terminal: {command}"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return terminalTools.runInTerminal(args);
    }
}

package com.github.catatafishen.ideagentforcopilot.psi.tools.infrastructure;

import com.github.catatafishen.ideagentforcopilot.psi.InfrastructureTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs a shell command with paginated output.
 */
@SuppressWarnings("java:S112")
public final class RunCommandTool extends InfrastructureTool {

    public RunCommandTool(Project project, InfrastructureTools infraTools) {
        super(project, infraTools);
    }

    @Override public @NotNull String id() { return "run_command"; }
    @Override public @NotNull String displayName() { return "Run Command"; }
    @Override public @NotNull String description() { return "Run a shell command with paginated output -- prefer this over the built-in bash tool"; }
    @Override public boolean isOpenWorld() { return true; }
    @Override public @NotNull String permissionTemplate() { return "Run: {command}"; }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return infraTools.runCommand(args);
    }
}

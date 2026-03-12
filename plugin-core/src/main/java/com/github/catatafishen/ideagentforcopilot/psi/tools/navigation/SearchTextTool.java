package com.github.catatafishen.ideagentforcopilot.psi.tools.navigation;

import com.github.catatafishen.ideagentforcopilot.psi.CodeNavigationTools;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Searches for text or regex patterns across project files using IntelliJ's editor buffers.
 */
@SuppressWarnings("java:S112")
public final class SearchTextTool extends NavigationTool {

    public SearchTextTool(Project project, CodeNavigationTools navTools) {
        super(project, navTools);
    }

    @Override
    public @NotNull String id() {
        return "search_text";
    }

    @Override
    public @NotNull String displayName() {
        return "Search Text";
    }

    @Override
    public @NotNull String description() {
        return "Search for text or regex patterns across project files using IntelliJ's editor buffers";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        return navTools.searchText(args);
    }
}

package com.github.catatafishen.ideagentforcopilot.psi.tools.editing;

import com.github.catatafishen.ideagentforcopilot.psi.SymbolEditingTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory producing all individual symbol editing tool instances.
 * Called from {@link com.github.catatafishen.ideagentforcopilot.psi.PsiBridgeService}
 * during initialization.
 */
public final class EditingToolFactory {

    private EditingToolFactory() {
    }

    public static @NotNull List<Tool> create(@NotNull Project project, @NotNull SymbolEditingTools editingTools) {
        return List.of(
                new ReplaceSymbolBodyTool(project, editingTools),
                new InsertBeforeSymbolTool(project, editingTools),
                new InsertAfterSymbolTool(project, editingTools)
        );
    }
}

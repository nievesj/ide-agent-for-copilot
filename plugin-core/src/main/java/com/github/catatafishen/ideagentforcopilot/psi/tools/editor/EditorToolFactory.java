package com.github.catatafishen.ideagentforcopilot.psi.tools.editor;

import com.github.catatafishen.ideagentforcopilot.psi.EditorTools;
import com.github.catatafishen.ideagentforcopilot.psi.tools.Tool;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory that creates all editor tool instances.
 */
public final class EditorToolFactory {

    private EditorToolFactory() {
    }

    public static @NotNull List<Tool> create(@NotNull Project project, @NotNull EditorTools editorTools) {
        return List.of(
                new OpenInEditorTool(project, editorTools),
                new ShowDiffTool(project, editorTools),
                new CreateScratchFileTool(project, editorTools),
                new ListScratchFilesTool(project, editorTools),
                new RunScratchFileTool(project, editorTools),
                new GetChatHtmlTool(project, editorTools),
                new GetActiveFileTool(project, editorTools),
                new GetOpenEditorsTool(project, editorTools),
                new ListThemesTool(project, editorTools),
                new SetThemeTool(project, editorTools),
                new SearchConversationHistoryTool(project, editorTools)
        );
    }
}

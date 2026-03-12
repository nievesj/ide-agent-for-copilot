package com.github.catatafishen.ideagentforcopilot.psi.tools.quality;

import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Returns quick-fix and intention action names available at a specific file and line.
 * Uses the cached daemon highlight data — no extra analysis is triggered.
 * Surfaces the same fixes shown in the IDE's light-bulb / import bubble.
 */
public final class GetAvailableActionsTool extends QualityTool {

    private static final Logger LOG = Logger.getInstance(GetAvailableActionsTool.class);

    public GetAvailableActionsTool(Project project) {
        super(project);
    }

    @Override
    public @NotNull String id() {
        return "get_available_actions";
    }

    @Override
    public @NotNull String displayName() {
        return "Get Available Actions";
    }

    @Override
    public @NotNull String description() {
        return "Get quick-fix and intention action names available at a specific file and line. "
            + "Returns the same fixes shown in the IDE's light-bulb / 'import' bubble. "
            + "Use apply_quickfix to apply one by inspection_id, or optimize_imports to fix all missing imports at once.";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public @Nullable JsonObject inputSchema() {
        return schema(new Object[][]{
            {"file", TYPE_STRING, "Path to the file"},
            {"line", TYPE_INTEGER, "Line number (1-based)"}
        }, "file", "line");
    }

    @Override
    public @Nullable String execute(@NotNull JsonObject args) throws Exception {
        if (!args.has("file") || !args.has("line")) {
            return "Error: 'file' and 'line' parameters are required";
        }
        String pathStr = args.get("file").getAsString();
        int targetLine = args.get("line").getAsInt();

        CompletableFuture<String> future = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() ->
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    future.complete(collectActionsAt(pathStr, targetLine));
                } catch (Exception e) {
                    LOG.warn("Error collecting actions at " + pathStr + ":" + targetLine, e);
                    future.complete("Error: " + e.getMessage());
                }
            })
        );
        return future.get(15, TimeUnit.SECONDS);
    }

    private String collectActionsAt(String pathStr, int targetLine) {
        VirtualFile vf = resolveVirtualFile(pathStr);
        if (vf == null) return "Error: File not found: " + pathStr;

        Document doc = FileDocumentManager.getInstance().getDocument(vf);
        if (doc == null) return "Error: Cannot get document for: " + pathStr;

        if (targetLine < 1 || targetLine > doc.getLineCount()) {
            return "Error: Line " + targetLine + " is out of bounds (file has " + doc.getLineCount() + " lines)";
        }

        int lineStart = doc.getLineStartOffset(targetLine - 1);
        int lineEnd = doc.getLineEndOffset(targetLine - 1);

        List<com.intellij.codeInsight.daemon.impl.HighlightInfo> allHighlights = new ArrayList<>();
        com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx.processHighlights(
            doc, project, null, 0, doc.getTextLength(), allHighlights::add);

        List<String> entries = new ArrayList<>();
        Set<String> allFixNames = new LinkedHashSet<>();

        for (var h : allHighlights) {
            // Include highlights whose range overlaps the target line
            if (h.getStartOffset() > lineEnd || h.getEndOffset() < lineStart) continue;
            String desc = h.getDescription();
            if (desc == null) continue;

            int actualLine = doc.getLineNumber(h.getStartOffset()) + 1;
            String severity = h.getSeverity().getName();
            List<String> fixes = collectQuickFixNames(h);

            String entry = pathStr + ":" + actualLine + " [" + severity + "] " + desc;
            if (!fixes.isEmpty()) {
                entry += "  →  Quick fixes: [" + String.join(", ", fixes) + "]";
                allFixNames.addAll(fixes);
            }
            entries.add(entry);
        }

        if (entries.isEmpty()) {
            return "No highlights found at " + pathStr + " line " + targetLine + ". "
                + "The daemon may not have analyzed this file yet — open it in the editor or call get_highlights first.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Actions available at ").append(pathStr).append(" line ").append(targetLine).append(":\n\n");
        sb.append(String.join("\n", entries));
        if (!allFixNames.isEmpty()) {
            sb.append("\n\nTo apply a fix: use apply_quickfix(file, line, inspection_id) "
                + "or optimize_imports for missing-import fixes.");
        }
        return sb.toString();
    }
}

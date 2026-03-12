package com.github.catatafishen.ideagentforcopilot.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Project-level service that owns the registry of all tool definitions.
 * <p>
 * Each open project gets its own registry (fixing the multi-project leakage bug
 * that existed when this was a static utility). Consumers obtain it via
 * {@link #getInstance(Project)}.
 * <p>
 * Tool instances are created by {@link com.github.catatafishen.ideagentforcopilot.psi.PsiBridgeService}
 * (which has access to package-private handler constructors) and injected here
 * via {@link #registerAll(Collection)}.
 */
public final class ToolRegistry {

    // ── Category enum (static — same across all projects) ────────────────

    public enum Category {
        FILE("File Operations"),
        SEARCH("Search & Navigation"),
        CODE_QUALITY("Code Quality"),
        BUILD("Build / Run / Test"),
        RUN("Terminal & Commands"),
        GIT("Git"),
        REFACTOR("Refactoring"),
        IDE("IDE & Project"),
        TESTING("Testing"),
        PROJECT("Project"),
        INFRASTRUCTURE("Infrastructure"),
        TERMINAL("Terminal"),
        EDITOR("Editor"),
        SHELL("Shell (built-in)"),
        OTHER("Other"),
        MACRO("Recorded Macros");

        public final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }
    }

    // ── Built-in tool entries (static — agent-side tools, same across all projects) ──

    record ToolEntry(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull String description,
        @NotNull Category category,
        boolean isBuiltIn,
        boolean hasDenyControl,
        boolean supportsPathSubPermissions
    ) implements ToolDefinition {
    }

    private static final List<ToolEntry> BUILT_IN_TOOLS = List.of(
        new ToolEntry("view", "Read File (built-in)", "Read file contents from disk (Copilot CLI built-in)", Category.FILE, true, false, true),
        new ToolEntry("read", "Read File (built-in)", "Read file contents from disk (built-in)", Category.FILE, true, false, true),
        new ToolEntry("grep", "Grep Search (built-in)", "Search file contents with regular expressions (built-in)", Category.SEARCH, true, false, false),
        new ToolEntry("glob", "Glob Find (built-in)", "Find files by name pattern (built-in)", Category.SEARCH, true, false, false),
        new ToolEntry("list", "List Files (built-in)", "List files and directories (OpenCode built-in)", Category.SEARCH, true, false, true),
        new ToolEntry("bash", "Bash Shell (built-in)", "Run arbitrary shell commands -- use run_command instead for safer, paginated execution", Category.SHELL, true, true, false),
        new ToolEntry("edit", "Edit File (built-in)", "Edit a file in place (built-in) -- use edit_text or replace_symbol_body for IDE-aware editing", Category.FILE, true, true, true),
        new ToolEntry("write", "Write File (built-in)", "Create or overwrite a file (OpenCode built-in) -- use intellij_write_file for IDE-aware writing", Category.FILE, true, true, true),
        new ToolEntry("create", "Create File (built-in)", "Create a new file (Copilot CLI built-in) -- use create_file for IDE-aware creation", Category.FILE, true, true, true),
        new ToolEntry("execute", "Execute (built-in)", "Execute a shell command (built-in)", Category.SHELL, true, true, false),
        new ToolEntry("runInTerminal", "Run in Terminal (built-in)", "Run a command in the integrated terminal (Copilot CLI built-in)", Category.SHELL, true, true, false)
    );

    private static final Map<String, String> BUILT_IN_PERMISSION_QUESTIONS =
        Map.of(
            "bash", "Run: {cmd}",
            "edit", "Edit {path}",
            "write", "Write {path}",
            "create", "Create {path}",
            "execute", "Execute: {command}",
            "runInTerminal", "Run in terminal: {command}"
        );

    // ── Instance state (project-scoped) ──────────────────────────────────

    private final Map<String, ToolDefinition> definitions = new LinkedHashMap<>();

    @SuppressWarnings("java:S1905") // Cast needed: IDE doesn't resolve Project→ComponentManager supertype
    public static ToolRegistry getInstance(@NotNull Project project) {
        return ((ComponentManager) project).getService(ToolRegistry.class);
    }

    @SuppressWarnings("unused") // instantiated by IntelliJ service container
    public ToolRegistry(@NotNull Project project) {
        // Tools are registered later by PsiBridgeService via registerAll()
    }

    // ── Registration ─────────────────────────────────────────────────────

    public void register(@NotNull ToolDefinition def) {
        definitions.put(def.id(), def);
    }

    public void unregister(@NotNull String id) {
        definitions.remove(id);
    }

    public void registerAll(@NotNull Collection<? extends ToolDefinition> defs) {
        for (ToolDefinition def : defs) {
            definitions.put(def.id(), def);
        }
    }

    // ── Lookups ──────────────────────────────────────────────────────────

    /**
     * Look up a tool definition by ID. Only searches MCP tools (not built-in agent tools).
     */
    @Nullable
    public ToolDefinition findDefinition(@NotNull String id) {
        return definitions.get(id);
    }

    /**
     * Look up a tool by ID (exact match). Checks MCP definitions first,
     * falls back to built-in tools list.
     */
    @Nullable
    public ToolDefinition findById(@Nullable String id) {
        if (id == null) return null;
        ToolDefinition def = definitions.get(id);
        if (def != null) return def;
        for (ToolEntry e : BUILT_IN_TOOLS) {
            if (e.id().equals(id)) return e;
        }
        return null;
    }

    /**
     * Returns all tools: built-in agent tools plus all registered MCP tool definitions.
     */
    @NotNull
    public List<ToolDefinition> getAllTools() {
        var all = new ArrayList<ToolDefinition>(BUILT_IN_TOOLS);
        all.addAll(definitions.values());
        return List.copyOf(all);
    }

    /**
     * Returns the IDs of all built-in agent tools (e.g., view, edit, bash).
     */
    @NotNull
    public static List<String> getBuiltInToolIds() {
        return BUILT_IN_TOOLS.stream().map(ToolEntry::id).toList();
    }

    /**
     * Returns MCP tool annotations for a given tool.
     */
    @NotNull
    public JsonObject getMcpAnnotations(@NotNull String toolId) {
        ToolDefinition def = findById(toolId);
        JsonObject ann = new JsonObject();
        if (def != null) {
            ann.addProperty("title", def.displayName());
            ann.addProperty("readOnlyHint", def.isReadOnly());
            ann.addProperty("destructiveHint", def.isDestructive());
            ann.addProperty("openWorldHint", def.isOpenWorld());
        }
        return ann;
    }

    // ── Permission question resolution ───────────────────────────────────

    /**
     * Resolves a human-readable permission question for the given tool and arguments.
     * Substitutes {@code {paramName}} placeholders with the corresponding argument values.
     */
    @Nullable
    public String resolvePermissionQuestion(@NotNull String toolId, @Nullable JsonObject args) {
        String template = resolveTemplate(toolId);
        if (template == null) return null;
        if (args == null) return stripPlaceholders(template);
        String q = substituteArgs(template, args);
        return stripPlaceholders(q);
    }

    @Nullable
    private String resolveTemplate(@NotNull String toolId) {
        ToolDefinition def = definitions.get(toolId);
        if (def != null && def.permissionTemplate() != null) {
            return def.permissionTemplate();
        }
        return BUILT_IN_PERMISSION_QUESTIONS.get(toolId);
    }

    private static String substituteArgs(@NotNull String template, @NotNull JsonObject args) {
        String q = template;
        for (Map.Entry<String, JsonElement> e : args.entrySet()) {
            q = q.replace("{" + e.getKey() + "}", formatArgValue(e.getValue()));
        }
        return q;
    }

    private static String formatArgValue(@NotNull JsonElement value) {
        if (value.isJsonNull()) return "";
        if (value.isJsonPrimitive()) {
            String s = value.getAsString();
            return s.length() > 60 ? s.substring(0, 57) + "…" : s;
        }
        if (value.isJsonArray()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (JsonElement el : value.getAsJsonArray()) {
                joiner.add(el.isJsonPrimitive() ? el.getAsString() : el.toString());
            }
            return joiner.toString();
        }
        return value.toString();
    }

    @Nullable
    private static String stripPlaceholders(@NotNull String text) {
        String q = text.replaceAll("\\{[^}]+}", "").replaceAll("\\(\\s*\\)", "")
            .replaceAll("\\s+", " ").trim();
        return q.isEmpty() ? null : q;
    }
}

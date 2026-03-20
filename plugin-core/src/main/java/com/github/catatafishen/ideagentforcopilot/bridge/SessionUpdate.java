package com.github.catatafishen.ideagentforcopilot.bridge;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Sealed type hierarchy for structured agent → UI update events and raw wire protocol entities.
 *
 * <p>Replaces the previous ad-hoc {@link com.google.gson.JsonObject} payloads, giving each
 * event a well-typed shape that the UI can pattern-match exhaustively.  The concrete type
 * always correlates to a {@link AgentClient.SessionUpdateType} enum value.</p>
 */
public sealed interface SessionUpdate
    permits SessionUpdate.ToolCall,
    SessionUpdate.ToolCallUpdate,
    SessionUpdate.AgentThought,
    SessionUpdate.TurnUsage,
    SessionUpdate.Banner,
    SessionUpdate.Plan {

    // ── Enums ─────────────────────────────────────────────────────────────────

    /**
     * Functional category of a tool call, used for UI styling.
     */
    enum ToolKind {
        @SerializedName("read") READ("read"),
        @SerializedName("edit") EDIT("edit"),
        @SerializedName("delete") DELETE("delete"),
        @SerializedName("move") MOVE("move"),
        @SerializedName("search") SEARCH("search"),
        @SerializedName("execute") EXECUTE("execute"),
        @SerializedName("think") THINK("think"),
        @SerializedName("fetch") FETCH("fetch"),
        @SerializedName("other") OTHER("other");

        private final String value;

        ToolKind(String value) {
            this.value = value;
        }

        /**
         * Wire value passed to the JS chat panel.
         */
        public String value() {
            return value;
        }

        /**
         * Returns the matching constant, or {@link #OTHER} for unknown values.
         */
        public static ToolKind fromString(@Nullable String s) {
            if (s == null) return OTHER;
            for (ToolKind k : values()) {
                if (k.value.equalsIgnoreCase(s) || k.name().equalsIgnoreCase(s)) return k;
            }
            return OTHER;
        }

        /**
         * Maps a {@link com.github.catatafishen.ideagentforcopilot.services.ToolRegistry.Category}
         * to a {@link ToolKind} for UI chip styling.
         */
        public static ToolKind fromCategory(@Nullable Object category) {
            if (category == null) return OTHER;
            String catName = category.toString();
            return switch (catName) {
                case "SEARCH" -> SEARCH;
                case "FILE", "EDITOR", "REFACTOR" -> EDIT;
                case "BUILD", "RUN", "TERMINAL", "SHELL", "GIT" -> EXECUTE;
                case "CODE_QUALITY", "TESTING", "IDE", "PROJECT", "INFRASTRUCTURE" -> READ;
                default -> OTHER;
            };
        }
    }

    /**
     * Terminal outcome of a tool call.
     */
    enum ToolCallStatus {
        @SerializedName("completed") COMPLETED("completed"),
        @SerializedName("failed") FAILED("failed"),
        @SerializedName("pending") PENDING("in_progress"),
        @SerializedName("in_progress") IN_PROGRESS("in_progress");

        private final String value;

        ToolCallStatus(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        /**
         * Returns the matching constant, or {@link #FAILED} for unknown values.
         */
        public static ToolCallStatus fromString(@Nullable String s) {
            if (s == null) return FAILED;
            for (ToolCallStatus st : values()) {
                if (st.value.equalsIgnoreCase(s) || st.name().equalsIgnoreCase(s)) return st;
            }
            if ("success".equalsIgnoreCase(s) || "succeeded".equalsIgnoreCase(s)) return COMPLETED;
            if ("in-progress".equalsIgnoreCase(s) || "in_progress".equalsIgnoreCase(s) || "running".equalsIgnoreCase(s))
                return IN_PROGRESS;
            return FAILED;
        }
    }

    /**
     * Visual severity of a banner notification.
     */
    enum BannerLevel {
        WARNING("warning"),
        ERROR("error");

        private final String value;

        BannerLevel(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        /**
         * Returns the matching constant, or {@link #WARNING} for unknown values.
         */
        public static BannerLevel fromString(@Nullable String s) {
            if (s == null) return WARNING;
            for (BannerLevel l : values()) {
                if (l.value.equalsIgnoreCase(s)) return l;
            }
            return WARNING;
        }
    }

    /**
     * Determines when a banner notification is automatically dismissed.
     */
    enum ClearOn {
        /**
         * Banner persists until the next successful prompt turn.
         */
        NEXT_SUCCESS("next_success"),
        /**
         * Banner is shown once and not re-shown automatically.
         */
        MANUAL("manual");

        private final String value;

        ClearOn(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        /**
         * Returns the matching constant, or {@link #MANUAL} for unknown values.
         */
        public static ClearOn fromString(@Nullable String s) {
            if (s == null) return MANUAL;
            for (ClearOn c : values()) {
                if (c.value.equalsIgnoreCase(s)) return c;
            }
            return MANUAL;
        }
    }

    // ── Event records ─────────────────────────────────────────────────────────

    /**
     * A new tool call has started.
     *
     * @param toolCallId          unique ID used to correlate with the matching {@link ToolCallUpdate}
     * @param title               normalised tool name (MCP prefix stripped)
     * @param kind                functional category of the tool
     * @param arguments           serialised JSON string of the tool arguments, or {@code null} if empty
     * @param filePaths           file paths extracted from the tool event (may be empty)
     * @param agentType           non-null when this is a Task/sub-agent tool call; contains the
     *                            agent-type string (e.g. {@code "general-purpose"})
     * @param subAgentDescription short description of the sub-agent task, or {@code null}
     * @param subAgentPrompt      the prompt sent to the sub-agent, or {@code null}
     */
    record ToolCall(
        @NotNull String toolCallId,
        @NotNull String title,
        @NotNull ToolKind kind,
        @Nullable String arguments,
        @NotNull List<String> filePaths,
        @Nullable String agentType,
        @Nullable String subAgentDescription,
        @Nullable String subAgentPrompt
    ) implements SessionUpdate {

        /**
         * Returns {@code true} when this tool call starts a sub-agent (Task tool).
         */
        public boolean isSubAgent() {
            return agentType != null;
        }
    }

    /**
     * A tool call has completed or failed.
     *
     * @param toolCallId  ID matching the originating {@link ToolCall}
     * @param status      terminal outcome of the call
     * @param result      result text for a completed call (may be {@code null})
     * @param error       error message for a failed call (may be {@code null})
     * @param description optional natural language explanation of the result (may be {@code null})
     */
    record ToolCallUpdate(
        @NotNull String toolCallId,
        @NotNull ToolCallStatus status,
        @Nullable String result,
        @Nullable String error,
        @Nullable String description
    ) implements SessionUpdate {
    }

    /**
     * A reasoning/thinking chunk from the model.
     *
     * @param text the thinking text fragment
     */
    record AgentThought(@NotNull String text) implements SessionUpdate {
    }

    /**
     * Turn-level token and cost statistics, emitted once per completed turn.
     *
     * @param inputTokens  total input tokens consumed
     * @param outputTokens total output tokens generated
     * @param costUsd      estimated cost in USD
     */
    record TurnUsage(int inputTokens, int outputTokens, double costUsd) implements SessionUpdate {
    }

    /**
     * An agent-initiated banner notification.
     *
     * @param message human-readable text to display
     * @param level   visual severity ({@link BannerLevel#WARNING} = yellow, {@link BannerLevel#ERROR} = red)
     * @param clearOn when the banner should be auto-dismissed
     */
    record Banner(
        @NotNull String message,
        @NotNull BannerLevel level,
        @NotNull ClearOn clearOn
    ) implements SessionUpdate {
    }

    /**
     * Extracts a plan update from an ACP agent, carrying a list of plan entries.
     *
     * @param plan the current plan
     */
    record Plan(@NotNull Protocol.Plan plan) implements SessionUpdate {

        /**
         * Returns the list of plan entries.
         */
        public @NotNull List<PlanEntry> entries() {
            return plan.entries != null ? plan.entries.stream()
                .map(e -> new PlanEntry(
                    e.content != null ? e.content : "Step",
                    e.status != null ? e.status : "pending",
                    e.priority != null ? e.priority : ""
                ))
                .toList() : List.of();
        }

        /**
         * A single entry in the agent plan.
         *
         * @param content  description of the step
         * @param status   step status string from the ACP protocol (e.g. {@code "pending"},
         *                 {@code "in_progress"}, {@code "done"}) — kept as {@code String}
         *                 because the ACP protocol may send values outside a fixed set
         * @param priority optional priority string, empty if not set
         */
        public record PlanEntry(
            @NotNull String content,
            @NotNull String status,
            @NotNull String priority
        ) {
        }
    }

    // ── Raw protocol classes ──────────────────────────────────────────────────

    /**
     * POJO classes for Agent Client Protocol (ACP) entities.
     * See https://agentclientprotocol.com/protocol/tool-calls
     */
    final class Protocol {

        /**
         * A tool call event from the agent.
         */
        public static class ToolCall {
            @SerializedName(value = "toolCallId", alternate = {"id"})
            public String toolCallId;

            @SerializedName("title")
            public String title;

            @SerializedName("kind")
            public ToolKind kind;

            /**
             * The arguments for the tool call. Can be a JSON object, array, or string depending on the tool.
             * Maps to "arguments", "input", or "rawInput".
             */
            @SerializedName(value = "arguments", alternate = {"input", "rawInput"})
            public Object arguments;

            /**
             * Thinking/reasoning from the agent.
             */
            @SerializedName("content")
            public Object content;

            /**
             * Metadata about the sub-agent if this is a task tool.
             */
            @SerializedName("metadata")
            public Map<String, Object> metadata;

            @SerializedName("locations")
            public List<Location> locations;

            public static class Location {
                @SerializedName("path")
                public String path;
                @SerializedName("range")
                public Range range;
            }

            public static class Range {
                @SerializedName("start")
                public Position start;
                @SerializedName("end")
                public Position end;
            }

            public static class Position {
                @SerializedName("line")
                public int line;
                @SerializedName("character")
                public int character;
            }
        }

        /**
         * A tool call update event (result or error).
         */
        public static class ToolCallUpdate {
            @SerializedName(value = "toolCallId", alternate = {"id"})
            public String toolCallId;

            @SerializedName("status")
            public ToolCallStatus status;

            /**
             * The result of the tool call.
             * Maps to "result" or "output".
             */
            @SerializedName(value = "result", alternate = {"output", "toolResult"})
            public Object result;

            /**
             * OpenCode specific nested result.
             */
            @SerializedName("rawOutput")
            public RawOutput rawOutput;

            public static class RawOutput {
                @SerializedName("output")
                public Object output;
            }

            /**
             * Error message if the tool call failed.
             */
            @SerializedName("error")
            public String error;

            /**
             * Optional natural language explanation of the result.
             */
            @SerializedName("description")
            public String description;

            /**
             * Thinking/reasoning from the agent during the update.
             */
            @SerializedName("content")
            public Object content;
        }

        /**
         * A plan update from the ACP agent, carrying a list of plan entries.
         */
        public static class Plan {
            @SerializedName("entries")
            public List<PlanEntry> entries;

            public static class PlanEntry {
                @SerializedName("content")
                public String content;
                @SerializedName("status")
                public String status;
                @SerializedName("priority")
                public String priority;
            }
        }
    }
}

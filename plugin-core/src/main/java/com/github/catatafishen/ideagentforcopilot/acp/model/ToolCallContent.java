package com.github.catatafishen.ideagentforcopilot.acp.model;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Content types produced by tool call executions.
 * Discriminated union — serialized with a "type" field.
 *
 * @see <a href="https://agentclientprotocol.com/protocol/tool-calls">ACP Tool Calls</a>
 */
public sealed interface ToolCallContent {

    /**
     * Standard content blocks (text, images, etc.) from a tool result.
     */
    record Content(List<ContentBlock> content) implements ToolCallContent {}

    /**
     * A file diff produced by a tool (file modifications).
     */
    record Diff(
            String path,
            @Nullable String oldText,
            String newText
    ) implements ToolCallContent {}

    /**
     * A terminal reference embedded in a tool call.
     */
    record Terminal(String terminalId) implements ToolCallContent {}
}

package com.github.catatafishen.ideagentforcopilot.acp.model;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Real-time session updates streamed from agent to client.
 * Discriminated union — serialized with a "sessionUpdate" field.
 *
 * @see <a href="https://agentclientprotocol.com/protocol/prompt-turn#3-agent-reports-output">ACP Session Updates</a>
 */
public sealed interface SessionUpdate {

    /**
     * Streamed text chunk from the agent's response.
     */
    record AgentMessageChunk(List<ContentBlock> content) implements SessionUpdate {}

    /**
     * Agent's internal reasoning (thinking/chain-of-thought).
     */
    record AgentThoughtChunk(List<ContentBlock> content) implements SessionUpdate {}

    /**
     * A new tool call initiated by the agent.
     */
    record ToolCall(
            String toolCallId,
            String title,
            @Nullable ToolKind kind,
            @Nullable String arguments,
            @Nullable List<Location> locations
    ) implements SessionUpdate {}

    /**
     * Status update for an in-progress tool call.
     */
    record ToolCallUpdate(
            String toolCallId,
            ToolCallStatus status,
            @Nullable List<ToolCallContent> content,
            @Nullable String error
    ) implements SessionUpdate {}

    /**
     * Agent's execution plan with task entries.
     */
    record Plan(List<PlanEntry> entries) implements SessionUpdate {}

    /**
     * Available commands have changed.
     */
    record AvailableCommandsChanged(
            List<NewSessionResponse.AvailableCommand> commands
    ) implements SessionUpdate {}

    /**
     * Available modes have changed.
     */
    record AvailableModesChanged(
            List<NewSessionResponse.AvailableMode> modes,
            @Nullable String activeSlug
    ) implements SessionUpdate {}
}

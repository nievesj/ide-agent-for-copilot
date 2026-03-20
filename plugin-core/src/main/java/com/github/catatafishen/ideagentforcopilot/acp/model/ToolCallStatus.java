package com.github.catatafishen.ideagentforcopilot.acp.model;

/**
 * Status of a tool call execution.
 *
 * @see <a href="https://agentclientprotocol.com/protocol/tool-calls">ACP Tool Calls</a>
 */
public enum ToolCallStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

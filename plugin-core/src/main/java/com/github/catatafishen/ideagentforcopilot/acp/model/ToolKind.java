package com.github.catatafishen.ideagentforcopilot.acp.model;

/**
 * Categorization of tool operations per the ACP spec.
 *
 * @see <a href="https://agentclientprotocol.com/protocol/tool-calls">ACP Tool Calls</a>
 */
public enum ToolKind {
    READ,
    EDIT,
    DELETE,
    MOVE,
    SEARCH,
    EXECUTE,
    THINK,
    FETCH,
    SWITCH_MODE,
    OTHER
}

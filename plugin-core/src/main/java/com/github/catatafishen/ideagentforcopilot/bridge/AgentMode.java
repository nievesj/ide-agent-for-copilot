package com.github.catatafishen.ideagentforcopilot.bridge;

import org.jetbrains.annotations.NotNull;

/**
 * Describes a session mode that an agent supports (e.g., "agent", "plan").
 * Each agent declares its own set of modes via {@link AgentConfig#getSupportedModes()}.
 *
 * @param id          internal identifier stored in settings (e.g., "agent", "plan")
 * @param displayName label shown in the toolbar dropdown (e.g., "Agent", "Plan")
 */
public record AgentMode(@NotNull String id, @NotNull String displayName) {
}

package com.github.catatafishen.ideagentforcopilot.agent;

import com.github.catatafishen.ideagentforcopilot.acp.client.CopilotClient;
import com.github.catatafishen.ideagentforcopilot.acp.client.JunieClient;
import com.github.catatafishen.ideagentforcopilot.acp.client.KiroClient;
import com.github.catatafishen.ideagentforcopilot.acp.client.OpenCodeClient;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Hardcoded registry of available agent clients.
 * No dynamic profiles — each agent is a known class.
 */
public final class AgentRegistry {

    /**
     * Describes an available agent without creating an instance.
     */
    public record AgentDescriptor(
            String id,
            String displayName,
            Function<Project, AgentConnector> factory
    ) {}

    private static final Map<String, AgentDescriptor> AGENTS = new LinkedHashMap<>();

    static {
        register("copilot", "GitHub Copilot", CopilotClient::new);
        register("junie", "Junie", JunieClient::new);
        register("kiro", "Kiro", KiroClient::new);
        register("opencode", "OpenCode", OpenCodeClient::new);
    }

    private AgentRegistry() {}

    private static void register(String id, String displayName,
                                 Function<Project, AgentConnector> factory) {
        AGENTS.put(id, new AgentDescriptor(id, displayName, factory));
    }

    /**
     * Get all available agent descriptors (in display order).
     */
    public static List<AgentDescriptor> getAll() {
        return List.copyOf(AGENTS.values());
    }

    /**
     * Get a descriptor by agent ID, or null if not found.
     */
    public static @Nullable AgentDescriptor get(String agentId) {
        return AGENTS.get(agentId);
    }

    /**
     * Create a new agent connector instance for the given agent ID.
     *
     * @param agentId the agent ID (e.g. "copilot")
     * @param project the IntelliJ project
     * @return a new connector instance, or null if the agent ID is unknown
     */
    public static @Nullable AgentConnector create(String agentId, Project project) {
        AgentDescriptor descriptor = AGENTS.get(agentId);
        return descriptor != null ? descriptor.factory().apply(project) : null;
    }
}

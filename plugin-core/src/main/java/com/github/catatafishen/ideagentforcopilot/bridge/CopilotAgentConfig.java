package com.github.catatafishen.ideagentforcopilot.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent configuration for GitHub Copilot CLI.
 * Handles Copilot-specific binary discovery, authentication parsing,
 * model metadata, and pre-launch setup (instructions, agents config).
 */
public class CopilotAgentConfig implements AgentConfig {

    private static final Logger LOG = Logger.getInstance(CopilotAgentConfig.class);
    private static final String DESCRIPTION = "description";
    private static final String META = "_meta";
    private static final String COMMAND = "command";

    private String resolvedBinaryPath;
    private JsonArray authMethods;

    @Override
    public @NotNull String getDisplayName() {
        return "Copilot";
    }

    @Override
    public @NotNull String getNotificationGroupId() {
        return "Copilot Notifications";
    }

    @Override
    public void prepareForLaunch(@Nullable String projectBasePath) {
        CopilotInstructionsManager.ensureInstructions(projectBasePath);
        CopilotAgentsManager.ensureAgents(projectBasePath);
    }

    @Override
    public @NotNull String findAgentBinary() throws AcpException {
        String path = CopilotCliLocator.findCopilotCli();
        resolvedBinaryPath = path;
        return path;
    }

    @Override
    public @NotNull ProcessBuilder buildAcpProcess(@NotNull String binaryPath,
                                                   @Nullable String projectBasePath,
                                                   int mcpPort) throws AcpException {
        resolvedBinaryPath = binaryPath;
        return CopilotCliLocator.buildAcpCommand(binaryPath, projectBasePath, mcpPort);
    }

    @Override
    public void parseInitializeResponse(@NotNull JsonObject result) {
        authMethods = result.has("authMethods") ? result.getAsJsonArray("authMethods") : null;
    }

    @Override
    public @Nullable String parseModelUsage(@Nullable JsonObject modelMeta) {
        if (modelMeta != null && modelMeta.has("copilotUsage")) {
            return modelMeta.get("copilotUsage").getAsString();
        }
        return null;
    }

    @Override
    public @Nullable AuthMethod getAuthMethod() {
        if (authMethods == null || authMethods.isEmpty()) return null;
        JsonObject first = authMethods.get(0).getAsJsonObject();
        AuthMethod method = new AuthMethod();
        method.setId(first.has("id") ? first.get("id").getAsString() : "");
        method.setName(first.has("name") ? first.get("name").getAsString() : "");
        method.setDescription(first.has(DESCRIPTION) ? first.get(DESCRIPTION).getAsString() : "");
        parseTerminalAuth(first, method);
        return method;
    }

    @Override
    public @Nullable String getAgentBinaryPath() {
        return resolvedBinaryPath;
    }

    @Override
    public @NotNull java.util.List<AgentMode> getSupportedModes() {
        return java.util.List.of(
            new AgentMode("agent", "Agent"),
            new AgentMode("plan", "Plan")
        );
    }

    /**
     * Copilot surfaces ResourceReference as metadata-only — content must be duplicated
     * as plain text so the model actually sees it.
     */
    @Override
    public boolean requiresResourceContentDuplication() {
        return true;
    }

    private void parseTerminalAuth(JsonObject jsonObject, AuthMethod method) {
        if (jsonObject.has(META)) {
            JsonObject meta = jsonObject.getAsJsonObject(META);
            if (meta.has("terminal-auth")) {
                JsonObject termAuth = meta.getAsJsonObject("terminal-auth");
                method.setCommand(termAuth.has(COMMAND) ? termAuth.get(COMMAND).getAsString() : null);
                if (termAuth.has("args")) {
                    List<String> args = new ArrayList<>();
                    for (JsonElement a : termAuth.getAsJsonArray("args")) {
                        args.add(a.getAsString());
                    }
                    method.setArgs(args);
                }
            }
        }
    }
}

package com.github.catatafishen.ideagentforcopilot.bridge;

import com.github.catatafishen.ideagentforcopilot.services.GenericSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Agent configuration for Google Gemini CLI.
 * Gemini uses flag-style ACP: {@code gemini --experimental-acp}.
 */
public class GeminiAgentConfig implements AgentConfig {

    private final GenericSettings settings;
    private String resolvedBinaryPath;
    private JsonArray authMethods;

    public GeminiAgentConfig(@NotNull GenericSettings settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Gemini";
    }

    @Override
    public @NotNull String getNotificationGroupId() {
        return "Copilot Notifications";
    }

    @Override
    public void prepareForLaunch(@Nullable String projectBasePath) {
        // Gemini does not require pre-launch instruction files.
    }

    @Override
    public @NotNull String findAgentBinary() throws AcpException {
        String path = GenericCliLocator.findBinary("gemini", "Gemini CLI",
            "Install with: npm install -g @google/gemini-cli");
        resolvedBinaryPath = path;
        return path;
    }

    @Override
    public @NotNull ProcessBuilder buildAcpProcess(@NotNull String binaryPath,
                                                   @Nullable String projectBasePath,
                                                   int mcpPort) {
        resolvedBinaryPath = binaryPath;
        // Gemini uses --experimental-acp flag; does not support --config-dir
        return GenericCliLocator.buildAcpCommand(
            binaryPath, "gemini",
            List.of("--experimental-acp"),
            settings.getSelectedModel(),
            projectBasePath,
            mcpPort,
            false, true);
    }

    @Override
    public void parseInitializeResponse(@NotNull JsonObject result) {
        authMethods = result.has("authMethods") ? result.getAsJsonArray("authMethods") : null;
    }

    @Override
    public @Nullable String parseModelUsage(@Nullable JsonObject modelMeta) {
        return null;
    }

    @Override
    public @Nullable AuthMethod getAuthMethod() {
        return KiroAgentConfig.parseStandardAuthMethod(authMethods);
    }

    @Override
    public @Nullable String getAgentBinaryPath() {
        return resolvedBinaryPath;
    }
}

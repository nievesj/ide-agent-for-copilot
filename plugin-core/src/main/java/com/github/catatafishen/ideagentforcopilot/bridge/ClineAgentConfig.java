package com.github.catatafishen.ideagentforcopilot.bridge;

import com.github.catatafishen.ideagentforcopilot.services.GenericSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Agent configuration for Cline CLI.
 * Cline uses flag-style ACP: {@code cline --acp}.
 */
public class ClineAgentConfig implements AgentConfig {

    private final GenericSettings settings;
    private String resolvedBinaryPath;
    private JsonArray authMethods;

    public ClineAgentConfig(@NotNull GenericSettings settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Cline";
    }

    @Override
    public @NotNull String getNotificationGroupId() {
        return "Copilot Notifications";
    }

    @Override
    public void prepareForLaunch(@Nullable String projectBasePath) {
        // Cline does not require pre-launch instruction files.
    }

    @Override
    public @NotNull String findAgentBinary() throws AcpException {
        String path = GenericCliLocator.findBinary("cline", "Cline CLI",
            "Install with: npm install -g cline");
        resolvedBinaryPath = path;
        return path;
    }

    @Override
    public @NotNull ProcessBuilder buildAcpProcess(@NotNull String binaryPath,
                                                   @Nullable String projectBasePath) {
        resolvedBinaryPath = binaryPath;
        // Cline uses --acp flag; does not support --config-dir
        return GenericCliLocator.buildAcpCommand(
            binaryPath, "cline",
            List.of("--acp"),
            settings.getSelectedModel(),
            projectBasePath,
            settings.getDisabledMcpToolIds(),
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

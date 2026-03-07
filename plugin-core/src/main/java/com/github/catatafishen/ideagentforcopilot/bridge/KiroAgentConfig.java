package com.github.catatafishen.ideagentforcopilot.bridge;

import com.github.catatafishen.ideagentforcopilot.services.GenericSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent configuration for Kiro CLI.
 * Kiro uses subcommand-style ACP: {@code kiro-cli acp}.
 */
public class KiroAgentConfig implements AgentConfig {

    private final GenericSettings settings;
    private String resolvedBinaryPath;
    private JsonArray authMethods;

    public KiroAgentConfig(@NotNull GenericSettings settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Kiro";
    }

    @Override
    public @NotNull String getNotificationGroupId() {
        return "Copilot Notifications";
    }

    @Override
    public void prepareForLaunch(@Nullable String projectBasePath) {
        // Kiro does not require pre-launch instruction files.
    }

    @Override
    public @NotNull String findAgentBinary() throws AcpException {
        String path = GenericCliLocator.findBinary("kiro-cli", "Kiro CLI",
            "Install with: npm install -g @anthropic-ai/kiro-cli");
        resolvedBinaryPath = path;
        return path;
    }

    @Override
    public @NotNull ProcessBuilder buildAcpProcess(@NotNull String binaryPath,
                                                   @Nullable String projectBasePath) {
        resolvedBinaryPath = binaryPath;
        return GenericCliLocator.buildAcpCommand(
            binaryPath, "kiro-cli",
            List.of("acp"),
            settings.getSelectedModel(),
            projectBasePath,
            settings.getDisabledMcpToolIds(),
            true, true);
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
        return parseStandardAuthMethod(authMethods);
    }

    @Override
    public @Nullable String getAgentBinaryPath() {
        return resolvedBinaryPath;
    }

    static @Nullable AuthMethod parseStandardAuthMethod(@Nullable JsonArray authMethods) {
        if (authMethods == null || authMethods.isEmpty()) return null;
        JsonObject first = authMethods.get(0).getAsJsonObject();
        AuthMethod method = new AuthMethod();
        method.setId(first.has("id") ? first.get("id").getAsString() : "");
        method.setName(first.has("name") ? first.get("name").getAsString() : "");
        method.setDescription(first.has("description") ? first.get("description").getAsString() : "");
        if (first.has("_meta")) {
            JsonObject meta = first.getAsJsonObject("_meta");
            if (meta.has("terminal-auth")) {
                JsonObject termAuth = meta.getAsJsonObject("terminal-auth");
                method.setCommand(termAuth.has("command") ? termAuth.get("command").getAsString() : null);
                if (termAuth.has("args")) {
                    List<String> args = new ArrayList<>();
                    for (JsonElement a : termAuth.getAsJsonArray("args")) {
                        args.add(a.getAsString());
                    }
                    method.setArgs(args);
                }
            }
        }
        return method;
    }
}

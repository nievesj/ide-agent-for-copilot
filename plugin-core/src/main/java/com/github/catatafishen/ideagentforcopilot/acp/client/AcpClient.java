package com.github.catatafishen.ideagentforcopilot.acp.client;

import com.github.catatafishen.ideagentforcopilot.acp.model.InitializeRequest;
import com.github.catatafishen.ideagentforcopilot.acp.model.InitializeResponse;
import com.github.catatafishen.ideagentforcopilot.acp.model.Model;
import com.github.catatafishen.ideagentforcopilot.acp.model.NewSessionResponse;
import com.github.catatafishen.ideagentforcopilot.acp.model.PromptRequest;
import com.github.catatafishen.ideagentforcopilot.acp.model.PromptResponse;
import com.github.catatafishen.ideagentforcopilot.acp.model.SessionUpdate;
import com.github.catatafishen.ideagentforcopilot.acp.transport.JsonRpcTransport;
import com.github.catatafishen.ideagentforcopilot.agent.AgentConnector;
import com.github.catatafishen.ideagentforcopilot.agent.AgentPromptException;
import com.github.catatafishen.ideagentforcopilot.agent.AgentSessionException;
import com.github.catatafishen.ideagentforcopilot.agent.AgentStartException;
import com.github.catatafishen.ideagentforcopilot.permissions.PermissionDefaults;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Abstract ACP protocol client.
 * <p>
 * Manages: initialization, authentication, sessions, prompts.
 * Agent-specific behavior via abstract/overridable methods in subclasses.
 * Each concrete subclass IS the agent definition — no separate profile needed.
 *
 * @see <a href="https://agentclientprotocol.com">Agent Client Protocol</a>
 */
public abstract class AcpClient implements AgentConnector {

    private static final Logger LOG = Logger.getInstance(AcpClient.class);

    private static final long INITIALIZE_TIMEOUT_SECONDS = 90;
    private static final long SESSION_TIMEOUT_SECONDS = 30;
    private static final long PROMPT_TIMEOUT_SECONDS = 600;
    private static final long AUTH_TIMEOUT_SECONDS = 30;
    private static final long STOP_TIMEOUT_SECONDS = 5;

    private static final String PROTOCOL_VERSION = "0.1";
    private static final String CLIENT_NAME = "AgentBridge";
    private static final String CLIENT_VERSION = "2.0.0";
    private static final String KEY_SESSION_ID = "sessionId";

    protected final Gson gson = new GsonBuilder().create();
    protected final JsonRpcTransport transport = new JsonRpcTransport();
    protected final Project project;

    private @Nullable Process agentProcess;
    private @Nullable InitializeResponse capabilities;
    private @Nullable String currentSessionId;
    private final List<Model> availableModels = new ArrayList<>();

    protected AcpClient(Project project) {
        this.project = project;
    }

    // ═══════════════════════════════════════════════════
    // Final protocol methods — subclasses cannot override
    // ═══════════════════════════════════════════════════

    @Override
    public final void start() throws AgentStartException {
        try {
            int mcpPort = resolveMcpPort();
            agentProcess = launchProcess(mcpPort);
            transport.start(agentProcess);
            registerHandlers();
            capabilities = initialize();
            authenticate();
            LOG.info(displayName() + " agent started successfully");
        } catch (Exception e) {
            stop();
            throw new AgentStartException("Failed to start " + displayName(), e);
        }
    }

    @Override
    public final void stop() {
        transport.stop();
        destroyProcess();
        agentProcess = null;
        capabilities = null;
        currentSessionId = null;
        availableModels.clear();
    }

    @Override
    public final boolean isConnected() {
        return transport.isAlive() && agentProcess != null && agentProcess.isAlive();
    }

    @Override
    public final String createSession(String cwd) throws AgentSessionException {
        try {
            int mcpPort = resolveMcpPort();

            JsonObject params = new JsonObject();
            params.addProperty("cwd", cwd);
            customizeNewSession(cwd, mcpPort, params);

            CompletableFuture<JsonElement> future = transport.sendRequest("session/new", params);
            JsonElement result = future.get(SESSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            NewSessionResponse response = gson.fromJson(result, NewSessionResponse.class);
            currentSessionId = response.sessionId();

            if (response.models() != null) {
                availableModels.clear();
                availableModels.addAll(response.models());
            }

            onSessionCreated(currentSessionId);
            return currentSessionId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentSessionException("Session creation interrupted for " + displayName(), e);
        } catch (Exception e) {
            throw new AgentSessionException("Failed to create session for " + displayName(), e);
        }
    }

    @Override
    public final void cancelSession(String sessionId) {
        JsonObject params = new JsonObject();
        params.addProperty(KEY_SESSION_ID, sessionId);
        transport.sendNotification("session/cancel", params);
    }

    @Override
    public final PromptResponse sendPrompt(PromptRequest request,
                                           Consumer<SessionUpdate> onUpdate) throws AgentPromptException {
        try {
            JsonObject params = gson.toJsonTree(request).getAsJsonObject();
            CompletableFuture<JsonElement> future = transport.sendRequest(
                    "session/prompt", params, PROMPT_TIMEOUT_SECONDS, TimeUnit.SECONDS
            );
            JsonElement result = future.get(PROMPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return gson.fromJson(result, PromptResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentPromptException("Prompt interrupted for " + displayName(), e);
        } catch (Exception e) {
            throw new AgentPromptException("Prompt failed for " + displayName(), e);
        }
    }

    @Override
    public final List<Model> getAvailableModels() {
        return Collections.unmodifiableList(availableModels);
    }

    @Override
    public final void setModel(String sessionId, String modelId) {
        JsonObject params = new JsonObject();
        params.addProperty(KEY_SESSION_ID, sessionId);
        params.addProperty("modelId", modelId);
        transport.sendRequest("session/set_model", params);
    }

    /**
     * Send a session/message notification to inject context (e.g. instructions).
     */
    protected final void sendSessionMessage(String sessionId, String text) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", text);

        JsonArray messages = new JsonArray();
        messages.add(msg);

        JsonObject params = new JsonObject();
        params.addProperty(KEY_SESSION_ID, sessionId);
        params.add("messages", messages);

        transport.sendNotification("session/message", params);
    }

    // ═══════════════════════════════════════════════════
    // Abstract/overridable methods — subclass hooks
    // ═══════════════════════════════════════════════════

    /** Build the command line to launch this agent process. */
    protected abstract List<String> buildCommand(String cwd, int mcpPort);

    /**
     * Extra environment variables for the agent process.
     * Override to add agent-specific env vars.
     *
     * @param mcpPort the MCP server port for this session
     */
    protected Map<String, String> buildEnvironment(@SuppressWarnings("unused") int mcpPort) {
        return Map.of();
    }

    /**
     * Customize the session/new request parameters.
     * Override to add MCP servers, metadata, etc.
     *
     * @param cwd     working directory for the session
     * @param mcpPort the MCP server port
     * @param params  the JSON params object to modify
     */
    @SuppressWarnings("unused")
    protected void customizeNewSession(String cwd, int mcpPort, JsonObject params) {
        // Default: no customization
    }

    /**
     * Called after a session is successfully created.
     * Override to inject instructions, etc.
     *
     * @param sessionId the created session ID
     */
    @SuppressWarnings("unused")
    protected void onSessionCreated(String sessionId) {
        // Default: no post-session setup
    }

    /**
     * Extract canonical tool ID from the agent's protocol tool call title.
     */
    protected String resolveToolId(String protocolTitle) {
        return protocolTitle;
    }

    /**
     * Whether context references must be inlined as text.
     */
    protected boolean requiresInlineReferences() {
        return false;
    }

    /**
     * Post-process a session update before delivering to UI.
     */
    protected SessionUpdate processUpdate(SessionUpdate update) {
        return update;
    }

    /** Default permission levels for each tool category. */
    protected PermissionDefaults permissionDefaults() {
        return PermissionDefaults.STANDARD;
    }

    /**
     * Resolve the MCP server port. Override to connect to MCP.
     */
    protected int resolveMcpPort() {
        return 0;
    }

    // ═══════════════════════════════════════════════════
    // Private protocol implementation
    // ═══════════════════════════════════════════════════

    private Process launchProcess(int mcpPort) throws IOException {
        String cwd = project.getBasePath();
        if (cwd == null) {
            cwd = System.getProperty("user.home");
        }

        List<String> command = buildCommand(cwd, mcpPort);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(cwd));
        pb.redirectErrorStream(false);

        Map<String, String> env = buildEnvironment(mcpPort);
        if (!env.isEmpty()) {
            pb.environment().putAll(env);
        }

        LOG.info("Launching " + displayName() + ": " + String.join(" ", command));
        return pb.start();
    }

    private InitializeResponse initialize() throws Exception {
        InitializeRequest request = new InitializeRequest(
                PROTOCOL_VERSION,
                new InitializeRequest.ClientInfo(CLIENT_NAME, CLIENT_VERSION),
                InitializeRequest.ClientCapabilities.standard()
        );

        JsonObject params = gson.toJsonTree(request).getAsJsonObject();
        CompletableFuture<JsonElement> future = transport.sendRequest(
                "initialize", params, INITIALIZE_TIMEOUT_SECONDS, TimeUnit.SECONDS
        );

        JsonElement result = future.get(INITIALIZE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        InitializeResponse response = gson.fromJson(result, InitializeResponse.class);

        LOG.info(displayName() + " initialized: " + response.agentInfo().name()
                + " v" + response.agentInfo().version());
        return response;
    }

    private void authenticate() throws Exception {
        if (capabilities == null || capabilities.authMethods() == null
                || capabilities.authMethods().isEmpty()) {
            return;
        }

        String methodId = capabilities.authMethods().getFirst().id();
        JsonObject params = new JsonObject();
        params.addProperty("methodId", methodId);

        CompletableFuture<JsonElement> future = transport.sendRequest(
                "authenticate", params, AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS
        );
        future.get(AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        LOG.info(displayName() + " authenticated with method: " + methodId);
    }

    private void registerHandlers() {
        transport.onNotification(notification -> {
            if ("session/update".equals(notification.method())) {
                handleSessionUpdate(notification.params());
            }
        });

        transport.onRequest(this::handleAgentRequest);

        transport.onStderr(line ->
                LOG.info("[" + agentId() + " stderr] " + line));
    }

    private void handleSessionUpdate(@Nullable JsonObject params) {
        if (params == null) return;
        LOG.debug("Session update: " + params);
    }

    private void handleAgentRequest(long id, JsonRpcTransport.IncomingRequest request) {
        switch (request.method()) {
            case "session/request_permission" -> handlePermissionRequest(id);
            case "fs/read_text_file", "fs/write_text_file",
                 "terminal/create", "terminal/output" ->
                    transport.sendError(id, -32603, request.method() + " not yet implemented");
            default -> {
                LOG.warn("Unknown agent request: " + request.method());
                transport.sendError(id, -32601, "Method not found: " + request.method());
            }
        }
    }

    private void handlePermissionRequest(long id) {
        JsonObject result = new JsonObject();
        JsonObject outcome = new JsonObject();
        outcome.addProperty("optionId", "allow_once");
        result.add("outcome", outcome);
        transport.sendResponse(id, result);
    }

    private void destroyProcess() {
        if (agentProcess == null || !agentProcess.isAlive()) {
            return;
        }
        agentProcess.destroy();
        try {
            if (!agentProcess.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                agentProcess.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            agentProcess.destroyForcibly();
        }
    }
}

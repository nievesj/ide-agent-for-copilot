package com.github.catatafishen.ideagentforcopilot.agent;

import com.github.catatafishen.ideagentforcopilot.acp.model.ContentBlock;
import com.github.catatafishen.ideagentforcopilot.acp.model.Model;
import com.github.catatafishen.ideagentforcopilot.acp.model.PromptRequest;
import com.github.catatafishen.ideagentforcopilot.acp.model.PromptResponse;
import com.github.catatafishen.ideagentforcopilot.acp.model.SessionUpdate;
import com.github.catatafishen.ideagentforcopilot.acp.model.ToolCallStatus;
import com.github.catatafishen.ideagentforcopilot.acp.model.ToolKind;
import com.github.catatafishen.ideagentforcopilot.acp.model.ToolCallContent;
import com.github.catatafishen.ideagentforcopilot.acp.model.PlanEntry;
import com.github.catatafishen.ideagentforcopilot.acp.model.Location;
import com.github.catatafishen.ideagentforcopilot.bridge.AcpException;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AgentClientAdapter}: verifies type conversions between
 * new {@code acp.model} types and legacy {@code bridge} types.
 */
@DisplayName("AgentClientAdapter")
class AgentClientAdapterTest {

    private StubConnector connector;
    private AgentClientAdapter adapter;

    @BeforeEach
    void setUp() {
        connector = new StubConnector();
        adapter = new AgentClientAdapter(connector);
    }

    // ── Lifecycle ───────────────────────────────────────

    @Nested
    @DisplayName("Lifecycle delegation")
    class LifecycleTests {

        @Test
        @DisplayName("start() delegates to connector")
        void startDelegates() throws AcpException {
            adapter.start();
            assertTrue(connector.started);
        }

        @Test
        @DisplayName("start() wraps AgentStartException as AcpException")
        void startWrapsException() {
            connector.startError = new AgentStartException("boom");
            assertThrows(AcpException.class, () -> adapter.start());
        }

        @Test
        @DisplayName("close() delegates to connector.stop()")
        void closeDelegates() {
            adapter.close();
            assertTrue(connector.stopped);
        }

        @Test
        @DisplayName("isHealthy() reflects connector.isConnected()")
        void isHealthyDelegates() {
            connector.connected = true;
            assertTrue(adapter.isHealthy());
            connector.connected = false;
            assertFalse(adapter.isHealthy());
        }
    }

    // ── Session ─────────────────────────────────────────

    @Nested
    @DisplayName("Session operations")
    class SessionTests {

        @Test
        @DisplayName("createSession() delegates and returns session ID")
        void createSessionDelegates() throws AcpException {
            connector.sessionId = "sess-42";
            assertEquals("sess-42", adapter.createSession("/some/path"));
            assertEquals("/some/path", connector.lastCwd);
        }

        @Test
        @DisplayName("createSession() wraps AgentSessionException")
        void createSessionWrapsException() {
            connector.sessionError = new AgentSessionException("no session");
            assertThrows(AcpException.class, () -> adapter.createSession(null));
        }

        @Test
        @DisplayName("cancelSession() delegates to connector")
        void cancelSessionDelegates() {
            adapter.cancelSession("sess-1");
            assertEquals("sess-1", connector.cancelledSessionId);
        }
    }

    // ── Models ──────────────────────────────────────────

    @Nested
    @DisplayName("Model conversion")
    class ModelTests {

        @Test
        @DisplayName("listModels() converts new Model to bridge.Model")
        void listModelsConverts() {
            connector.models = List.of(
                    new Model("gpt-4", "GPT-4", "The best one", null),
                    new Model("gpt-3.5", "GPT-3.5", null, null)
            );

            var result = adapter.listModels();
            assertEquals(2, result.size());
            assertEquals("gpt-4", result.getFirst().getId());
            assertEquals("GPT-4", result.getFirst().getName());
            assertEquals("The best one", result.getFirst().getDescription());
            assertEquals("gpt-3.5", result.get(1).getId());
        }

        @Test
        @DisplayName("getModelMultiplier() returns default when not found")
        void getModelMultiplierDefault() {
            assertEquals("1x", adapter.getModelMultiplier("unknown-model"));
        }
    }

    // ── sendPrompt ──────────────────────────────────────

    @Nested
    @DisplayName("sendPrompt")
    class SendPromptTests {

        @Test
        @DisplayName("returns stop reason from PromptResponse")
        void returnsStopReason() throws AcpException {
            connector.promptResponse = new PromptResponse("max_tokens", null);
            String result = adapter.sendPrompt("sess-1", "hello", null, null, null, null, null);
            assertEquals("max_tokens", result);
        }

        @Test
        @DisplayName("defaults to end_turn when stop reason is null")
        void defaultsToEndTurn() throws AcpException {
            connector.promptResponse = new PromptResponse(null, null);
            String result = adapter.sendPrompt("sess-1", "hello", null, null, null, null, null);
            assertEquals("end_turn", result);
        }

        @Test
        @DisplayName("dispatches AgentMessageChunk text to onChunk")
        void dispatchesTextChunk() throws AcpException {
            connector.updatesToSend = List.of(
                    new SessionUpdate.AgentMessageChunk(List.of(
                            new ContentBlock.Text("Hello "),
                            new ContentBlock.Text("world")
                    ))
            );
            connector.promptResponse = new PromptResponse("end_turn", null);

            List<String> chunks = new ArrayList<>();
            adapter.sendPrompt("sess-1", "hi", null, null, chunks::add, null, null);

            assertEquals(List.of("Hello ", "world"), chunks);
        }

        @Test
        @DisplayName("dispatches ToolCall update to onUpdate")
        void dispatchesToolCall() throws AcpException {
            connector.updatesToSend = List.of(
                    new SessionUpdate.ToolCall("tc-1", "Read file", ToolKind.READ, "{}", List.of(
                            new Location("file:///foo.java", null)
                    ))
            );
            connector.promptResponse = new PromptResponse("end_turn", null);

            AtomicReference<com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate> captured =
                    new AtomicReference<>();
            adapter.sendPrompt("sess-1", "test", null, null, null, captured::set, null);

            assertInstanceOf(com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate.ToolCall.class,
                    captured.get());

            var tc = (com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate.ToolCall) captured.get();
            assertEquals("tc-1", tc.toolCallId());
            assertEquals("Read file", tc.title());
            assertEquals(com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate.ToolKind.READ, tc.kind());
            assertEquals(List.of("file:///foo.java"), tc.filePaths());
        }

        @Test
        @DisplayName("dispatches ToolCallUpdate with content extraction")
        void dispatchesToolCallUpdate() throws AcpException {
            connector.updatesToSend = List.of(
                    new SessionUpdate.ToolCallUpdate("tc-1", ToolCallStatus.COMPLETED,
                            List.of(new ToolCallContent.Content(List.of(new ContentBlock.Text("result text")))),
                            null)
            );
            connector.promptResponse = new PromptResponse("end_turn", null);

            AtomicReference<com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate> captured =
                    new AtomicReference<>();
            adapter.sendPrompt("sess-1", "test", null, null, null, captured::set, null);

            var tcu = (com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate.ToolCallUpdate) captured.get();
            assertEquals("tc-1", tcu.toolCallId());
            assertEquals("result text", tcu.result());
        }

        @Test
        @DisplayName("dispatches AgentThought to onUpdate")
        void dispatchesThought() throws AcpException {
            connector.updatesToSend = List.of(
                    new SessionUpdate.AgentThoughtChunk(List.of(new ContentBlock.Text("thinking...")))
            );
            connector.promptResponse = new PromptResponse("end_turn", null);

            AtomicReference<com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate> captured =
                    new AtomicReference<>();
            adapter.sendPrompt("sess-1", "test", null, null, null, captured::set, null);

            var thought = (com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate.AgentThought) captured.get();
            assertEquals("thinking...", thought.text());
        }

        @Test
        @DisplayName("dispatches Plan entries")
        void dispatchesPlan() throws AcpException {
            connector.updatesToSend = List.of(
                    new SessionUpdate.Plan(List.of(
                            new PlanEntry("Step 1", "done"),
                            new PlanEntry("Step 2", "pending")
                    ))
            );
            connector.promptResponse = new PromptResponse("end_turn", null);

            AtomicReference<com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate> captured =
                    new AtomicReference<>();
            adapter.sendPrompt("sess-1", "test", null, null, null, captured::set, null);

            var plan = (com.github.catatafishen.ideagentforcopilot.bridge.SessionUpdate.Plan) captured.get();
            assertEquals(2, plan.plan().entries.size());
            assertEquals("Step 1", plan.plan().entries.getFirst().content);
            assertEquals("done", plan.plan().entries.getFirst().status);
        }

        @Test
        @DisplayName("calls onRequest before sending prompt")
        void callsOnRequest() throws AcpException {
            connector.promptResponse = new PromptResponse("end_turn", null);
            boolean[] called = {false};
            adapter.sendPrompt("sess-1", "hi", null, null, null, null, () -> called[0] = true);
            assertTrue(called[0]);
        }

        @Test
        @DisplayName("wraps AgentPromptException as AcpException")
        void wrapsPromptException() {
            connector.promptError = new AgentPromptException("timeout");
            assertThrows(AcpException.class,
                    () -> adapter.sendPrompt("sess-1", "hi", null, null, null, null, null));
        }
    }

    // ── Stub connector ──────────────────────────────────

    private static class StubConnector implements AgentConnector {
        boolean started;
        boolean stopped;
        boolean connected;
        String sessionId = "default-session";
        String lastCwd;
        String cancelledSessionId;
        List<Model> models = List.of();
        PromptResponse promptResponse = new PromptResponse("end_turn", null);
        List<SessionUpdate> updatesToSend = List.of();

        @Nullable AgentStartException startError;
        @Nullable AgentSessionException sessionError;
        @Nullable AgentPromptException promptError;

        @Override
        public String agentId() {
            return "test-agent";
        }

        @Override
        public String displayName() {
            return "Test Agent";
        }

        @Override
        public void start() throws AgentStartException {
            if (startError != null) throw startError;
            started = true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public String createSession(String cwd) throws AgentSessionException {
            if (sessionError != null) throw sessionError;
            lastCwd = cwd;
            return sessionId;
        }

        @Override
        public void cancelSession(String sessionId) {
            cancelledSessionId = sessionId;
        }

        @Override
        public PromptResponse sendPrompt(PromptRequest request,
                                         Consumer<SessionUpdate> onUpdate) throws AgentPromptException {
            if (promptError != null) throw promptError;
            for (SessionUpdate update : updatesToSend) {
                onUpdate.accept(update);
            }
            return promptResponse;
        }

        @Override
        public List<Model> getAvailableModels() {
            return models;
        }

        @Override
        public void setModel(String sessionId, String modelId) {
            // no-op
        }
    }
}

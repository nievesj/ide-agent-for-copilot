package com.github.catatafishen.ideagentforcopilot.bridge;

import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.GenericSettings;
import com.github.catatafishen.ideagentforcopilot.services.ToolPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JunieAcpClientWhitelistTest {

    private JunieAcpClient client;

    @BeforeEach
    void setUp() {
        AgentProfile profile = JunieAcpClient.createDefaultProfile();
        AgentConfig config = new ProfileBasedAgentConfig(profile, null);
        AgentSettings settings = new GenericAgentSettings(new GenericSettings("junie"), null);
        client = new JunieAcpClient(config, settings, null, null, 0);
    }

    private SessionUpdate.Protocol.ToolCall createToolCall(String title) {
        SessionUpdate.Protocol.ToolCall toolCall = new SessionUpdate.Protocol.ToolCall();
        toolCall.toolCallId = "id";
        toolCall.title = title;
        toolCall.kind = SessionUpdate.ToolKind.OTHER;
        return toolCall;
    }

    @Test
    void testWhitelistAllowsAgentBridgeTools() {
        SessionUpdate.Protocol.ToolCall toolCall = createToolCall("Tool: agentbridge/read_file");
        assertEquals(ToolPermission.ALLOW, client.resolveEffectivePermission(toolCall),
            "Should allow agentbridge- prefixed tools");

        SessionUpdate.Protocol.ToolCall toolCall2 = createToolCall("Tool: agentbridge/search_text");
        assertEquals(ToolPermission.ALLOW, client.resolveEffectivePermission(toolCall2),
            "Should allow agentbridge- prefixed tools");
    }

    @Test
    void testWhitelistDeniesKnownBuiltIns() {
        SessionUpdate.Protocol.ToolCall toolCall = createToolCall("Tool: bash");
        assertEquals(ToolPermission.DENY, client.resolveEffectivePermission(toolCall),
            "Should deny built-in bash tool");

        SessionUpdate.Protocol.ToolCall toolCall2 = createToolCall("Tool: read_file");
        assertEquals(ToolPermission.DENY, client.resolveEffectivePermission(toolCall2),
            "Should deny built-in read_file tool");

        SessionUpdate.Protocol.ToolCall toolCall3 = createToolCall("Tool: execute");
        assertEquals(ToolPermission.DENY, client.resolveEffectivePermission(toolCall3),
            "Should deny built-in execute tool");
    }

    @Test
    void testWhitelistDeniesUnknownTools() {
        SessionUpdate.Protocol.ToolCall toolCall = createToolCall("Tool: unknown_tool");
        assertEquals(ToolPermission.DENY, client.resolveEffectivePermission(toolCall),
            "Should deny unknown tools by default");

        SessionUpdate.Protocol.ToolCall toolCall2 = createToolCall("Tool: some_random_builtin");
        assertEquals(ToolPermission.DENY, client.resolveEffectivePermission(toolCall2),
            "Should deny unknown built-in tools");
    }

    @Test
    void testGetToolId() {
        SessionUpdate.Protocol.ToolCall toolCall = createToolCall("Tool: agentbridge/read_file");
        assertEquals("read_file", client.getToolId(toolCall));

        SessionUpdate.Protocol.ToolCall toolCall2 = createToolCall("Tool: read_file");
        assertEquals("read_file", client.getToolId(toolCall2));

        SessionUpdate.Protocol.ToolCall toolCall3 = createToolCall("bash");
        assertEquals("bash", client.getToolId(toolCall3));
    }
}

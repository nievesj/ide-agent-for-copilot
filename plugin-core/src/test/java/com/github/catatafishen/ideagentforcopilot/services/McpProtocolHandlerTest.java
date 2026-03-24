package com.github.catatafishen.ideagentforcopilot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the MCP protocol handler, including the resources surface.
 */
@SuppressWarnings("DataFlowIssue")
class McpProtocolHandlerTest {

    private McpProtocolHandler handler;

    @BeforeEach
    void setUp() {
        handler = new McpProtocolHandler(null);
    }

    @Test
    void testInitializeAdvertisesResourcesCapability() {
        JsonObject response = parseResponse(sendRequest("initialize", new JsonObject()));
        JsonObject capabilities = response.getAsJsonObject("result").getAsJsonObject("capabilities");

        assertNotNull(capabilities.getAsJsonObject("resources"));
        assertFalse(capabilities.getAsJsonObject("resources").get("listChanged").getAsBoolean());
    }

    @Test
    void testResourcesListIncludesStartupInstructions() {
        JsonObject response = parseResponse(sendRequest("resources/list", new JsonObject()));
        JsonArray resources = response.getAsJsonObject("result").getAsJsonArray("resources");

        assertEquals(1, resources.size());
        JsonObject resource = resources.get(0).getAsJsonObject();
        assertEquals("resource://default-startup-instructions.md", resource.get("uri").getAsString());
        assertEquals("default-startup-instructions", resource.get("name").getAsString());
        assertEquals("text/markdown", resource.get("mimeType").getAsString());
    }

    @Test
    void testResourcesReadReturnsStartupInstructionsText() {
        JsonObject params = new JsonObject();
        params.addProperty("uri", "resource://default-startup-instructions.md");

        JsonObject response = parseResponse(sendRequest("resources/read", params));
        JsonArray contents = response.getAsJsonObject("result").getAsJsonArray("contents");

        assertEquals(1, contents.size());
        JsonObject content = contents.get(0).getAsJsonObject();
        assertEquals("resource://default-startup-instructions.md", content.get("uri").getAsString());
        assertEquals("text/markdown", content.get("mimeType").getAsString());
        assertNotNull(content.get("text").getAsString());
        assertFalse(content.get("text").getAsString().isEmpty());
    }

    private String sendRequest(String method, JsonObject params) {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 1);
        request.addProperty("method", method);
        request.add("params", params);
        return handler.handleMessage(request.toString());
    }

    private JsonObject parseResponse(String json) {
        assertNotNull(json);
        return JsonParser.parseString(json).getAsJsonObject();
    }
}

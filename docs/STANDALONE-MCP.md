# Standalone MCP Server

The **IDE MCP Server** plugin exposes your JetBrains IDE as an MCP (Model Context Protocol)
HTTP server — any AI agent can use IntelliJ's 92 code intelligence tools without needing the
full AgentBridge plugin.

## When to Use

| Scenario | Plugin |
|----------|--------|
| You want a chat panel inside the IDE with agent profiles | **AgentBridge** (main plugin) |
| You want to connect an external agent (Claude Code, Claude Desktop, Cursor, etc.) to IDE tools | **IDE MCP Server** (this plugin) |
| You want both | Install both — they don't conflict |

## Installation

Download `ide-mcp-server-<version>.zip` from the
[Releases](https://github.com/catatafishen/intellij-copilot-plugin/releases) page.

**Settings → Plugins → ⚙ → Install Plugin from Disk** → select the ZIP.

## Configuration

After installation, configure the server in **Settings → Tools → MCP Server**:

- **Port** — HTTP port for the MCP server (default: auto-assigned)
- **Auto-start** — Start the server when the IDE opens
- **Transport** — Streamable HTTP or SSE
- **Tools** — Enable/disable individual tools

## Connecting Agents

### Claude Code

Add the MCP server to Claude Code's configuration:

```bash
claude mcp add --transport http intellij-tools http://localhost:<port>/mcp
```

Or add to `~/.claude.json`:

```json
{
  "mcpServers": {
    "intellij-tools": {
      "type": "http",
      "url": "http://localhost:<port>/mcp"
    }
  }
}
```

### Claude Desktop

Add to Claude Desktop's MCP config (`~/Library/Application Support/Claude/claude_desktop_config.json`
on macOS, `%APPDATA%\Claude\claude_desktop_config.json` on Windows):

```json
{
  "mcpServers": {
    "intellij-tools": {
      "type": "http",
      "url": "http://localhost:<port>/mcp"
    }
  }
}
```

### Cursor / Windsurf / Other MCP Clients

Point any MCP-compatible client at `http://localhost:<port>/mcp` using HTTP transport.

### Using the Stdio Server Directly

If your agent requires stdio transport instead of HTTP, you can run the bundled MCP server
JAR directly:

```bash
java -jar mcp-server.jar --port <port>
```

The JAR bridges stdio MCP ↔ HTTP, forwarding tool calls to the IDE's PSI bridge.

## Available Tools

All 92 tools from AgentBridge are available. See [FEATURES.md](../FEATURES.md) for the
complete list with descriptions.

## Toolbar Toggle

The plugin adds an **MCP Server** toggle to the main toolbar. Click to start/stop the
server without opening settings.

## Building from Source

```bash
./gradlew :standalone-mcp:buildPlugin
```

Output: `standalone-mcp/build/distributions/ide-mcp-server-<version>.zip`

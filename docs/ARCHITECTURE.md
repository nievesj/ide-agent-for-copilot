# Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     IntelliJ IDEA IDE                            │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              AgentBridge Plugin (Java 21)              │    │
│  │                                                          │    │
│  │  ┌──────────────┐  ┌───────────────────────────────┐  │    │
│  │  │ Tool Window  │  │  Services Layer               │  │    │
│  │  │  (JCEF Chat) │  │                               │  │    │
│  │  │              │  │  - ActiveAgentManager         │  │    │
│  │  │ • Chat panel │◄─┤  - AgentProfileManager        │  │    │
│  │  │ • Toolbar    │  │  - PsiBridgeService           │  │    │
│  │  │ • Prompt     │  │  - ToolChipRegistry           │  │    │
│  │  │              │  │                               │  │    │
│  │  └──────────────┘  └─────┬─────────────────────────┘  │    │
│  │                           │                             │    │
│  │  ┌────────────────────────▼──────────────────────┐    │    │
│  │  │     Agent Clients (AbstractAgentClient)       │    │    │
│  │  │                                               │    │    │
│  │  │  ACP-based:              Claude-based:        │    │    │
│  │  │  ├─ CopilotClient        ├─ ClaudeCliClient   │    │    │
│  │  │  ├─ JunieClient          └─ AnthropicDirect   │    │    │
│  │  │  ├─ KiroClient                                │    │    │
│  │  │  └─ OpenCodeClient                            │    │    │
│  │  └────────────────────┬──────────────────────────┘    │    │
│  │                       │                                │    │
│  │  ┌────────────────────▼──────────────────────┐        │    │
│  │  │     PSI Bridge (PsiBridgeService)         │        │    │
│  │  │  • HTTP server inside IntelliJ process    │        │    │
│  │  │  • 92 MCP tools via IntelliJ APIs         │        │    │
│  │  └────────────────────┬──────────────────────┘        │    │
│  └───────────────────────┼───────────────────────────────┘    │
│                          │ stdin/stdout or HTTP                │
└──────────────────────────┼────────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │   Agent CLI / API       │
              │   (Copilot/Junie/Kiro/  │
              │    OpenCode/Claude)     │
              └────────────┬────────────┘
                           │ stdio
              ┌────────────▼────────────┐
              │   MCP Server (JAR)      │
              │   (routes to PSI Bridge)│
              └────────────┬────────────┘
                           │ HTTP
              ┌────────────▼────────────┐
              │   Cloud LLM Provider    │
              │   (OpenAI/Anthropic/etc)│
              └─────────────────────────┘
```

---

## Component Details

### 1. Services Layer

#### ActiveAgentManager (Project-level)

Central service that manages the active agent profile and client lifecycle:

- Stores which `AgentProfile` is currently active
- Creates and owns the `AbstractAgentClient` for the active profile
- Handles client start/stop/restart/dispose
- Provides shared UI preferences (attach trigger, follow-agent-files)

```java
@Service(Service.Level.PROJECT)
public final class ActiveAgentManager implements Disposable {
    private AbstractAgentClient acpClient;
    private AgentConfig cachedConfig;
    
    public void setActiveProfile(AgentProfile profile);
    public AbstractAgentClient getClient();
    public void startAgent();
    public void stopAgent();
}
```

#### AgentProfileManager (Application-level)

Manages the collection of available agent profiles:

- Built-in profiles: GitHub Copilot, OpenCode, Junie, Kiro, Claude Code
- Custom user-defined profiles
- Profile persistence and serialization

#### PsiBridgeService (Project-level)

HTTP server exposing 92 IntelliJ-native MCP tools:

- Starts on dynamic localhost port
- Handles tool invocations from MCP server
- Accesses PSI, VFS, Document API, Git4Idea, etc.

### 2. Agent Clients

All agent clients extend `AbstractAgentClient`, which provides common functionality:

- Session management (create, prompt, cancel)
- Model selection
- Event streaming and listeners
- Connection lifecycle

#### ACP-Based Clients

For agents that use the Agent Client Protocol (JSON-RPC 2.0 over stdio):

| Client | Agent | Notes |
|--------|-------|-------|
| `CopilotClient` | GitHub Copilot CLI | Full ACP, permission requests |
| `JunieClient` | JetBrains Junie | ACP without permissions |
| `KiroClient` | Amazon Kiro | ACP with tool filtering |
| `OpenCodeClient` | OpenCode | ACP with config-based permissions |

#### Claude-Based Clients

For Anthropic Claude agents (different protocol):

| Client | Agent | Notes |
|--------|-------|-------|
| `ClaudeCliClient` | Claude Code CLI | Uses claude-code-acp wrapper |
| `AnthropicDirectClient` | Anthropic API | Direct API calls, no CLI |

### 3. Profile-Based Configuration

`ProfileBasedAgentConfig` creates agent configuration from an `AgentProfile`:

- CLI command and arguments
- Environment variables (MCP server config, API keys)
- Tool filtering (excludedTools parameter)
- Permission injection method (CLI flags, JSON config, or none)
- Session instructions

### 4. MCP Tool Bridge

```
Agent CLI ──stdio──► MCP Server (JAR) ──HTTP──► PsiBridgeService
                     agentbridge               (IntelliJ process)
```

- **MCP Server** (`mcp-server/`): Standalone JAR, stdio protocol, routes tool calls to PSI bridge
- **PSI Bridge** (`PsiBridgeService`): HTTP server inside IntelliJ process, accesses PSI/VFS/Document APIs
- **Bridge file**: `~/.copilot/psi-bridge.json` contains port for HTTP connection

---

## Data Flow

### Typical Prompt Flow

```
┌─────────┐            ┌────────┐           ┌─────────────┐
│  User   │            │ Plugin │           │  Agent CLI  │
└────┬────┘            └───┬────┘           └──────┬──────┘
     │                     │                       │
     │  Type prompt        │                       │
     ├────────────────────►│                       │
     │                     │  session/prompt        │
     │                     ├──────────────────────►│
     │                     │                       │
     │                     │  session/update        │
     │                     │◄──────────────────────┤
     │                     │  (streaming chunks)    │
     │  Display response   │                       │
     │◄────────────────────┤                       │
     │                     │                       │
     │                     │  MCP tool call         │
     │                     │◄──────────────────────┤
     │                     │  (write_file)          │
     │                     ├──────────────────────►│
     │                     │                       │
```

### Permission Flow (ACP agents with permissions)

```
Agent requests permission (kind="edit")
    → Plugin denies (forces MCP tool usage)
    → Agent retries with MCP tool (write_file)
    → Write goes through Document API
    → Auto-format runs
```

---

## Key Files

| File | Purpose |
|------|---------|
| `services/ActiveAgentManager.java` | Active profile and client lifecycle |
| `services/AgentProfileManager.java` | Profile collection management |
| `acp/client/AcpClient.java` | Base ACP client (JSON-RPC 2.0) |
| `acp/client/CopilotClient.java` | GitHub Copilot implementation |
| `acp/client/JunieClient.java` | JetBrains Junie implementation |
| `agent/claude/ClaudeCliClient.java` | Claude Code CLI implementation |
| `bridge/ProfileBasedAgentConfig.java` | Profile → AgentConfig conversion |
| `psi/PsiBridgeService.java` | 92 MCP tools via IntelliJ APIs |

---

## Security Considerations

### Tool Permissions

- **deny**: Never execute (fail immediately)
- **ask**: Prompt user for approval
- **allow**: Execute without prompt

### Sensitive Operations

Always require approval:
- `git_push --force`
- `run_command` (shell commands)
- File deletions
- Operations outside project root

---

*Last Updated: 2026-03-22*

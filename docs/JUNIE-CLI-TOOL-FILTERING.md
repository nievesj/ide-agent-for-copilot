# Junie CLI Tool Filtering Limitation

## The Bug

Similar to GitHub Copilot CLI (see `CLI-BUG-556-WORKAROUND.md`), the Junie CLI does not currently respect the
`excludedTools` parameter in the `session/new` Agent Communication Protocol (ACP) payload.

Even when the plugin explicitly requests the exclusion of built-in tools (e.g. `view`, `edit`, `bash`, `create`, `open`,
etc.) by passing them in the `session/new` request, the Junie agent will ignore this configuration and still register
and have access to these built-in tools.

> **UPDATE (March 17, 2026):** According
> to [JUNIE-1842](https://youtrack.jetbrains.com/issue/JUNIE-1842/Toolset-configuration-profiles-allow-deny-list), toolset
> configuration profiles with allow/deny lists are now supported. This should allow for a native mechanism to exclude
> tools at launch time instead of relying purely on runtime permission denial.

## Our Workaround

Since we cannot filter the tools natively via the CLI startup or session parameters, we rely on the same **runtime
permission denial** mechanism used for the Copilot CLI.

### Implementation

**File:** `plugin-core/src/main/java/com/github/catatafishen/ideagentforcopilot/bridge/JunieAcpClient.java`

We have updated the profile configuration for Junie to enforce plugin-level tool permissions:

```java
p.setExcludeAgentBuiltInTools(true);
p.

setUsePluginPermissions(true);
p.

setPermissionInjectionMethod(PermissionInjectionMethod.NONE);
```

By enabling `usePluginPermissions = true`, the plugin actively intercepts permission requests for built-in tools (such
as write/execute operations) and denies them, forcing Junie to use the IntelliJ MCP alternatives which are far superior
for editor contexts.

> **UPDATE (March 19, 2026):** Native tool filtering is now implemented following the resolution
> of [JUNIE-1842](https://youtrack.jetbrains.com/issue/JUNIE-1842/Toolset-configuration-profiles-allow-deny-list). The
> plugin now sends multiple tool-filtering parameters in the `session/new` request to ensure compatibility with Junie
> v888.212+:
> - `excludedTools`: `["glob", "grep", ...]` (Legacy Copilot format)
> - `toolFilter.denyList`: `["glob", "grep", ...]` (Structured format)
> - `allowList.deny`: `["glob", "grep", ...]` (Explicit allow-list system format)
>
> This multi-layered injection ensures that Junie correctly identifies and disables its built-in tools in favor of the
> IntelliJ MCP alternatives.

### Limitations

- **No CLI Flags:** Junie v888.212 was tested with `--help`, and it does not support any native CLI flags for tool
  exclusion (e.g., `--deny-tool` or `--exclude-tool`). Filtering MUST be done via the `session/new` ACP payload.
- **Legacy versions:** Junie versions older than 888.212 still ignore the `excludedTools` parameter. For these versions,
  we rely on the prompt engineering workaround (see `JUNIE-TOOL-WORKAROUND.md`) and runtime permission denial (for
  agents that support it).

## Tracking Changes

This file exists to track the limitation. If JetBrains releases a future version of the Junie CLI that successfully
honors the `excludedTools` field in the ACP `session/new` payload, we should verify it and eventually remove this
workaround.

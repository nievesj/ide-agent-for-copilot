package com.github.catatafishen.ideagentforcopilot.acp.model;

import org.jetbrains.annotations.Nullable;

/**
 * Client → Agent: protocol handshake.
 *
 * @see <a href="https://agentclientprotocol.com/protocol/initialization">ACP Initialization</a>
 */
public record InitializeRequest(
        String protocolVersion,
        ClientInfo clientInfo,
        ClientCapabilities clientCapabilities
) {

    public record ClientInfo(String name, String version) {}

    public record ClientCapabilities(
            @Nullable FsCapabilities fs,
            @Nullable Boolean terminal
    ) {
        public static ClientCapabilities standard() {
            return new ClientCapabilities(
                    new FsCapabilities(true, true),
                    true
            );
        }
    }

    public record FsCapabilities(
            @Nullable Boolean readTextFile,
            @Nullable Boolean writeTextFile
    ) {}
}

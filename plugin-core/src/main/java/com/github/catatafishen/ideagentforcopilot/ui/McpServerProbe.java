package com.github.catatafishen.ideagentforcopilot.ui;

import com.github.catatafishen.ideagentforcopilot.psi.PlatformApiCompat;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Checks whether the MCP HTTP server (from the standalone-mcp plugin) is running.
 * Uses {@link PlatformApiCompat#getPluginClassLoader} so that {@code Class.forName}
 * resolves across IntelliJ's plugin isolation boundary.
 */
final class McpServerProbe {

    private static final String MCP_PLUGIN_ID = "com.github.catatafishen.idemcpserver";
    private static final String MCP_SERVER_CLASS = "com.github.catatafishen.idemcpserver.McpHttpServer";

    private McpServerProbe() {
    }

    static boolean isRunning(@NotNull Project project) {
        try {
            ClassLoader mcpClassLoader = PlatformApiCompat.getPluginClassLoader(MCP_PLUGIN_ID);
            if (mcpClassLoader == null) return false;

            Class<?> serverClass = Class.forName(MCP_SERVER_CLASS, true, mcpClassLoader);
            Object server = serverClass.getMethod("getInstance", Project.class).invoke(null, project);
            if (server == null) return false;
            return (boolean) serverClass.getMethod("isRunning").invoke(server);
        } catch (Exception e) {
            return false;
        }
    }
}

package com.github.catatafishen.ideagentforcopilot.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts the PSI Bridge HTTP server when a project opens.
 * This ensures the bridge is available before any MCP tool calls arrive.
 */
public class PsiBridgeStartup implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(PsiBridgeStartup.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Starting PSI Bridge for project: " + project.getName());

        // Log dynamic plugin diagnostic info
        checkDynamicPluginStatus();

        // Create agent workspace directory structure
        createAgentWorkspace(project);

        PsiBridgeService.getInstance(project).start();
    }

    /**
     * Diagnostic: check if this plugin can be dynamically loaded/unloaded.
     * Logs the result so we can troubleshoot "restart required" issues.
     */
    @SuppressWarnings("all")
    private void checkDynamicPluginStatus() {
        LOG.info("Dynamic plugin check: starting diagnostic...");
        try {
            PluginId pluginId = PluginId.getId("com.github.catatafishen.ideagentforcopilot");

            // Try PluginManagerCore via reflection
            Class<?> pmcClass = Class.forName("com.intellij.ide.plugins.PluginManagerCore");
            Method findPlugin = null;
            Object descriptor = null;

            // Try static method first (Java-style)
            try {
                findPlugin = pmcClass.getMethod("findPlugin", PluginId.class);
                descriptor = findPlugin.invoke(null, pluginId);
            } catch (Exception e1) {
                LOG.info("Dynamic plugin check: static findPlugin failed (" + e1.getMessage()
                    + "), trying INSTANCE...");
                // Try Kotlin object style
                try {
                    Object pmcInstance = pmcClass.getField("INSTANCE").get(null);
                    findPlugin = pmcClass.getMethod("findPlugin", PluginId.class);
                    descriptor = findPlugin.invoke(pmcInstance, pluginId);
                } catch (Exception e2) {
                    LOG.warn("Dynamic plugin check: both approaches failed", e2);
                    return;
                }
            }

            if (descriptor == null) {
                LOG.warn("Dynamic plugin check: descriptor is null");
                return;
            }

            LOG.info("Dynamic plugin check: descriptor class=" + descriptor.getClass().getName());

            // Log version and requireRestart
            try {
                Method getVersion = descriptor.getClass().getMethod("getVersion");
                LOG.info("Dynamic plugin check: version=" + getVersion.invoke(descriptor));
            } catch (Exception e) {
                LOG.info("Dynamic plugin check: getVersion failed: " + e.getMessage());
            }

            try {
                Method isRequireRestart = descriptor.getClass().getMethod("isRequireRestart");
                LOG.info("Dynamic plugin check: isRequireRestart=" + isRequireRestart.invoke(descriptor));
            } catch (Exception e) {
                LOG.info("Dynamic plugin check: isRequireRestart failed: " + e.getMessage());
            }

            // Try DynamicPlugins.checkCanUnloadWithoutRestart
            try {
                Class<?> dpClass = Class.forName("com.intellij.ide.plugins.DynamicPlugins");
                Object dpInstance = dpClass.getField("INSTANCE").get(null);
                for (Method m : dpClass.getMethods()) {
                    if (m.getName().equals("checkCanUnloadWithoutRestart")) {
                        LOG.info("Dynamic plugin check: found method " + m.getName()
                            + " params=" + java.util.Arrays.toString(m.getParameterTypes()));
                    }
                }
                Method checkCanUnload = dpClass.getMethod("checkCanUnloadWithoutRestart",
                    descriptor.getClass());
                String reason = (String) checkCanUnload.invoke(dpInstance, descriptor);
                LOG.info("Dynamic plugin check: checkCanUnload="
                    + (reason != null ? reason : "null (OK - can unload dynamically)"));
            } catch (Exception e) {
                LOG.info("Dynamic plugin check: checkCanUnload failed: " + e.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Dynamic plugin check failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the .agent-work/ directory structure for agent session state.
     * This directory is typically gitignored and provides a safe workspace
     * for the agent to store plans, checkpoints, and analysis files.
     */
    private void createAgentWorkspace(@NotNull Project project) {
        if (project.getBasePath() == null) {
            return;
        }

        try {
            Path agentWork = Path.of(project.getBasePath(), ".agent-work");
            Path checkpoints = agentWork.resolve("checkpoints");
            Path files = agentWork.resolve("files");

            Files.createDirectories(checkpoints);
            Files.createDirectories(files);

            // Create plan.md if it doesn't exist
            Path planFile = agentWork.resolve("plan.md");
            if (!Files.exists(planFile)) {
                String template = """
                    # Agent Work Plan

                    ## Project Principles

                    When working on this IntelliJ plugin project:
                    - **Write clean, well-formatted code** following Java/Kotlin best practices
                    - **Use IntelliJ tools first**: `intellij_read_file`, `intellij_write_file`, `search_symbols`, `find_references`
                    - **Always format and optimize** after changes: `format_code` + `optimize_imports`
                    - **Test before commit**: `build_project` + `run_tests` to ensure nothing breaks
                    - **Make logical commits**: Group related changes, separate unrelated changes

                    ## Multi-Step Task Workflow

                    When fixing multiple issues:
                    1. Scan and group by problem TYPE (not by file)
                    2. Fix ONE problem type completely (may span multiple files)
                    3. Format, build, test, commit
                    4. ⚠️ **STOP and ASK** before continuing to next type

                    ## Current Tasks

                    _Use checkboxes below to track your progress:_

                    - [ ] Task 1
                    - [ ] Task 2

                    ## Notes

                    _Add any context, decisions, or findings here_
                    """;
                Files.writeString(planFile, template);
            }

            LOG.info("Agent workspace initialized at: " + agentWork);
        } catch (IOException e) {
            LOG.warn("Failed to create agent workspace", e);
        }
    }
}

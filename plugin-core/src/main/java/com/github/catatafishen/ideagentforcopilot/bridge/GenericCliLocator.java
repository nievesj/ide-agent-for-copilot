package com.github.catatafishen.ideagentforcopilot.bridge;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared CLI locator for ACP-compatible agents.
 * Finds a CLI binary by name on the system and builds an ACP ProcessBuilder.
 *
 * <p>Agents that need custom discovery logic (Copilot, Claude) keep their own
 * locators; new agents with standard npm/go installs reuse this class.</p>
 */
final class GenericCliLocator {

    private static final Logger LOG = Logger.getInstance(GenericCliLocator.class);

    private GenericCliLocator() {
    }

    /**
     * Find a CLI binary on the system by name.
     *
     * @param binaryName        the executable name (e.g., "kiro-cli", "gemini", "opencode", "cline")
     * @param displayName       human-readable name for error messages (e.g., "Kiro CLI")
     * @param installInstructions how to install (e.g., "npm i -g @google/gemini-cli")
     * @return absolute path to the binary
     * @throws AcpException if the binary cannot be found
     */
    @NotNull
    static String findBinary(@NotNull String binaryName, @NotNull String displayName,
                             @NotNull String installInstructions) throws AcpException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String pathResult = checkInPath(binaryName, isWindows);
        if (pathResult != null) return pathResult;

        if (!isWindows) {
            String unixPath = checkUnixLocations(binaryName);
            if (unixPath != null) return unixPath;
        }

        throw new AcpException(displayName + " not found. " + installInstructions, null, false);
    }

    @Nullable
    private static String checkInPath(@NotNull String binaryName, boolean isWindows) {
        try {
            String command = isWindows ? "where" : "which";
            Process check = new ProcessBuilder(command, binaryName).start();
            if (check.waitFor() == 0) {
                String path = new String(check.getInputStream().readAllBytes()).trim().split("\\r?\\n")[0];
                if (new File(path).exists()) {
                    LOG.info("Found " + binaryName + " in PATH: " + path);
                    return path;
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to check for " + binaryName + " in PATH", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted while checking for " + binaryName + " in PATH", e);
        }
        return null;
    }

    @Nullable
    private static String checkUnixLocations(@NotNull String binaryName) {
        String home = System.getProperty("user.home");
        List<String> candidates = new ArrayList<>();

        addNvmCandidates(home, binaryName, candidates);

        candidates.add(home + "/.local/bin/" + binaryName);
        candidates.add("/usr/local/bin/" + binaryName);
        candidates.add(home + "/.npm-global/bin/" + binaryName);
        candidates.add(home + "/.yarn/bin/" + binaryName);
        candidates.add("/opt/homebrew/bin/" + binaryName);
        // Go install location
        candidates.add(home + "/go/bin/" + binaryName);

        for (String path : candidates) {
            if (new File(path).exists()) {
                LOG.info("Found " + binaryName + " at: " + path);
                return path;
            }
        }
        return null;
    }

    private static void addNvmCandidates(@NotNull String home, @NotNull String binaryName,
                                         @NotNull List<String> candidates) {
        File nvmDir = new File(home, ".nvm/versions/node");
        if (nvmDir.isDirectory()) {
            File[] nodeDirs = nvmDir.listFiles(File::isDirectory);
            if (nodeDirs != null) {
                java.util.Arrays.sort(nodeDirs, (a, b) -> b.getName().compareTo(a.getName()));
                for (File nodeDir : nodeDirs) {
                    candidates.add(new File(nodeDir, "bin/" + binaryName).getAbsolutePath());
                }
            }
        }
    }

    /**
     * Resolve the node binary when a CLI is installed via NVM and add the command to the list.
     */
    static void addNodeAndCommand(@NotNull List<String> cmd, @NotNull String cliPath,
                                  @NotNull String binaryName) {
        if (cliPath.contains("/.nvm/versions/node/") && cliPath.contains("/bin/" + binaryName)) {
            String nodeDir = cliPath.substring(0, cliPath.indexOf("/bin/" + binaryName));
            String nodePath = nodeDir + "/bin/node";
            if (new File(nodePath).exists()) {
                cmd.add(nodePath);
            }
        }
        cmd.add(cliPath);
    }

    /**
     * Build an ACP ProcessBuilder for a generic agent.
     *
     * @param cliPath         absolute path to the CLI binary
     * @param binaryName      binary name (for NVM resolution)
     * @param acpArgs         ACP activation args (e.g., ["--acp"] or ["acp"])
     * @param savedModel      previously selected model, or null
     * @param projectBasePath project root directory, or null
     * @param disabledToolIds comma-separated disabled MCP tool IDs
     * @param supportsConfigDir whether the CLI supports --config-dir
     * @param supportsMcpConfig whether the CLI supports --additional-mcp-config
     * @return configured ProcessBuilder
     */
    static ProcessBuilder buildAcpCommand(@NotNull String cliPath,
                                          @NotNull String binaryName,
                                          @NotNull List<String> acpArgs,
                                          @Nullable String savedModel,
                                          @Nullable String projectBasePath,
                                          @NotNull String disabledToolIds,
                                          boolean supportsConfigDir,
                                          boolean supportsMcpConfig) {
        List<String> cmd = new ArrayList<>();

        addNodeAndCommand(cmd, cliPath, binaryName);
        cmd.addAll(acpArgs);

        if (savedModel != null && !savedModel.isEmpty()) {
            cmd.add("--model");
            cmd.add(savedModel);
            LOG.info(binaryName + " model set to: " + savedModel);
        }

        if (supportsConfigDir && projectBasePath != null) {
            Path agentWorkPath = Path.of(projectBasePath, ".agent-work");
            cmd.add("--config-dir");
            cmd.add(agentWorkPath.toString());
            LOG.info(binaryName + " config-dir set to: " + agentWorkPath);
        }

        if (supportsMcpConfig) {
            CopilotCliLocator.addMcpConfigFlags(cmd, projectBasePath, disabledToolIds);
        }

        return new ProcessBuilder(cmd);
    }
}

package com.github.catatafishen.ideagentforcopilot.settings;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Binary detector for the {@code gh} CLI, used for Copilot billing data.
 * Reads the user-configured path from {@link BillingSettings}, then falls back to
 * auto-detection via the shell environment and known install locations.
 */
public final class GhBinaryDetector extends ClientBinaryDetector {

    @Override
    @Nullable
    protected String getConfiguredPath() {
        String custom = BillingSettings.getInstance().getGhBinaryPath();
        return (custom == null || custom.isBlank()) ? null : custom.trim();
    }

    /**
     * Resolves the {@code gh} CLI path, also checking known OS-specific install
     * locations when PATH-based detection fails.
     */
    @Nullable
    public String resolveGh() {
        String found = resolve("gh");
        if (found != null) return found;
        // Last-resort: check known install locations not always in PATH
        return knownPaths().stream()
            .filter(p -> new File(p).canExecute())
            .findFirst()
            .orElse(null);
    }

    private List<String> knownPaths() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return List.of(
                "C:\\Program Files\\GitHub CLI\\gh.exe",
                "C:\\Program Files (x86)\\GitHub CLI\\gh.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\GitHub CLI\\gh.exe"
            );
        }
        if (os.contains("mac")) {
            return List.of(
                "/opt/homebrew/bin/gh",
                "/usr/local/bin/gh",
                "/usr/bin/gh"
            );
        }
        return List.of(
            "/usr/bin/gh",
            "/usr/local/bin/gh",
            System.getProperty("user.home") + "/.local/bin/gh",
            "/snap/bin/gh",
            "/home/linuxbrew/.linuxbrew/bin/gh"
        );
    }
}

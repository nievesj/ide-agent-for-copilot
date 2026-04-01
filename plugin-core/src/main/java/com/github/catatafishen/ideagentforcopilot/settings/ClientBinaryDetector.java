package com.github.catatafishen.ideagentforcopilot.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for per-client binary detection.
 * Checks the user's configured override path first, then falls back to
 * auto-detection via {@link BinaryDetector}.
 */
public abstract class ClientBinaryDetector {

    /**
     * Return the user-configured binary path override, or {@code null} if none is set.
     */
    @Nullable
    protected abstract String getConfiguredPath();

    /**
     * Resolve the binary: returns the configured override if set, otherwise
     * auto-detects using the captured shell environment.
     *
     * @param primaryName    Primary binary name (e.g. "copilot")
     * @param alternateNames Alternate names to try when primary is not found
     * @return Absolute path or name found, or {@code null} if not found
     */
    @Nullable
    public final String resolve(@NotNull String primaryName, @NotNull String... alternateNames) {
        String configured = getConfiguredPath();
        if (configured != null) {
            return configured;
        }
        String found = BinaryDetector.findBinaryPath(primaryName);
        if (found != null) return found;
        for (String alt : alternateNames) {
            found = BinaryDetector.findBinaryPath(alt);
            if (found != null) return found;
        }
        return null;
    }
}

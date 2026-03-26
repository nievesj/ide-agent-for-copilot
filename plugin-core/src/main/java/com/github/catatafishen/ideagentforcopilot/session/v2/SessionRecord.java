package com.github.catatafishen.ideagentforcopilot.session.v2;

import org.jetbrains.annotations.NotNull;

/**
 * Metadata entry for a single session stored in {@code sessions-index.json}.
 */
public final class SessionRecord {

    /** UUID of this session (also the stem of the JSONL filename). */
    @NotNull
    public final String id;

    /** Human-readable agent name, e.g. {@code "GitHub Copilot"}. */
    @NotNull
    public final String agent;

    /** Absolute path of the project that owns this session. */
    @NotNull
    public final String directory;

    /** Unix epoch millis — time the session was first created. */
    public final long createdAt;

    /** Unix epoch millis — time of the most-recent write. */
    public final long updatedAt;

    /**
     * Filename (no directory) of the JSONL file for this session,
     * e.g. {@code "550e8400-e29b-41d4-a716-446655440000.jsonl"}.
     */
    @NotNull
    public final String jsonlPath;

    public SessionRecord(
            @NotNull String id,
            @NotNull String agent,
            @NotNull String directory,
            long createdAt,
            long updatedAt,
            @NotNull String jsonlPath) {
        this.id = id;
        this.agent = agent;
        this.directory = directory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.jsonlPath = jsonlPath;
    }

    /** Returns a copy with an updated {@code updatedAt} timestamp. */
    @NotNull
    public SessionRecord withUpdatedAt(long newUpdatedAt) {
        return new SessionRecord(id, agent, directory, createdAt, newUpdatedAt, jsonlPath);
    }
}

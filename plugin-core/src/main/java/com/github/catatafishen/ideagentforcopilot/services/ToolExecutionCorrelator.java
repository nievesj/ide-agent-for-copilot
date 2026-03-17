package com.github.catatafishen.ideagentforcopilot.services;

import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Correlates MCP tool executions with agent tool result updates.
 * 
 * <h2>Problem Statement</h2>
 * Tool execution happens through two separate channels:
 * <ol>
 *   <li><b>MCP Protocol:</b> Agent → tools/call → McpProtocolHandler → PsiBridgeService → tool.execute()
 *       <br>Returns raw result (diffs, file contents, etc.)</li>
 *   <li><b>Agent Protocol:</b> Agent → tool_call_update event with status/description
 *       <br>May or may not include the raw result</li>
 * </ol>
 * 
 * <p><b>Claude-based agents</b> (ClaudeCliClient, AnthropicDirectClient):
 * <ul>
 *   <li>Execute tools themselves and return raw results in tool_result events</li>
 *   <li>✅ No correlation needed - we get the raw result directly</li>
 * </ul>
 * 
 * <p><b>ACP agents</b> (Junie, OpenCode, Copilot):
 * <ul>
 *   <li>Request permission, execute via MCP, but may not forward raw results</li>
 *   <li>Junie only sends natural language summaries: "Two files staged"</li>
 *   <li>⚠️ Need to correlate MCP execution with agent update to show raw data</li>
 * </ul>
 * 
 * <h2>Solution Strategy</h2>
 * <ol>
 *   <li><b>Record:</b> Capture every MCP tool execution (tool name, args, result, timestamp)</li>
 *   <li><b>Synchronize:</b> File/git/editing tools run one at a time per tool name (prevents race conditions)</li>
 *   <li><b>Match:</b> When agent sends tool_call_update, find matching execution by:
 *     <ul>
 *       <li>Primary: Exact match on tool name + args hash</li>
 *       <li>Fallback: FIFO (first unconsumed execution for this tool within 30s)</li>
 *     </ul>
 *   </li>
 *   <li><b>Consume:</b> Mark execution as consumed to prevent double-matching</li>
 *   <li><b>Degrade gracefully:</b> If no match found, use agent's description as fallback</li>
 * </ol>
 * 
 * <h2>Synchronization Design</h2>
 * Tools in FILE, EDITING, REFACTOR, GIT categories execute synchronously (one at a time per tool name).
 * This prevents:
 * <ul>
 *   <li>Race conditions when multiple edits happen concurrently</li>
 *   <li>Ambiguous matching when same tool called multiple times rapidly</li>
 *   <li>EDT queue saturation (multiple write operations flooding invokeLater)</li>
 * </ul>
 * 
 * Read-only tools (navigation, search, etc.) run concurrently - they're safe and fast.
 * Slow background tools (build, test) run concurrently - blocking would freeze the agent.
 * 
 * <h2>Debugging</h2>
 * Enable DEBUG logging to see:
 * <ul>
 *   <li>When tools acquire/release sync locks</li>
 *   <li>Execution recording: tool name, args hash, duration, queue size</li>
 *   <li>Matching attempts: exact match vs FIFO fallback, success/failure</li>
 *   <li>Consumption tracking: which execution was matched to which update</li>
 * </ul>
 * 
 * Example log output:
 * <pre>
 * [DEBUG] Acquired sync lock for git_status
 * [DEBUG] Recorded execution: git_status (hash=a3f5e1b2, duration=45ms, queueSize=1)
 * [DEBUG] Released sync lock for git_status
 * [DEBUG] Matched execution by args hash: git_status (hash=a3f5e1b2)
 * </pre>
 */
@Service(Service.Level.PROJECT)
public final class ToolExecutionCorrelator {
    private static final Logger LOG = Logger.getInstance(ToolExecutionCorrelator.class);

    /**
     * Tool categories that require synchronous execution (one at a time per tool name).
     * These are typically write operations or tools that modify IDE state.
     */
    private static final Set<String> SYNC_TOOL_CATEGORIES = Set.of(
        "FILE",      // read_file, write_file, create_file, delete_file
        "EDITING",   // edit_text, replace_symbol_body, insert_before_symbol
        "REFACTOR",  // refactor (rename, extract, inline, safe_delete)
        "GIT"        // git_stage, git_commit, git_reset (writes); git_status, git_diff (consistency)
    );

    /**
     * How long to keep pending executions before cleanup (milliseconds).
     * Should be longer than any reasonable tool execution + update delay.
     */
    private static final long RETENTION_MS = 60_000; // 1 minute

    /**
     * Time window for matching executions to updates (milliseconds).
     * Updates arriving after this window won't match executions.
     */
    private static final long MATCH_WINDOW_MS = 30_000; // 30 seconds

    /**
     * A recorded tool execution waiting to be matched with an agent update.
     * 
     * @param toolName Normalized tool name (e.g., "git_status")
     * @param args Original arguments passed to the tool
     * @param argsHash Hash of args for fast matching
     * @param result Raw tool output (diffs, file contents, etc.)
     * @param timestamp When execution completed (for time-bounded matching)
     * @param consumed Atomic flag to prevent double-matching (CAS ensures only one update matches)
     */
    record PendingExecution(
        String toolName,
        JsonObject args,
        String argsHash,
        String result,
        long timestamp,
        AtomicBoolean consumed
    ) {}

    /**
     * Per-tool FIFO queues of pending executions.
     * Preserves execution order for FIFO fallback matching.
     */
    private final Map<String, ConcurrentLinkedQueue<PendingExecution>> pendingByTool = new ConcurrentHashMap<>();

    /**
     * Per-tool locks for synchronous execution.
     * Only created for tools in SYNC_TOOL_CATEGORIES.
     */
    private final Map<String, ReentrantLock> toolLocks = new ConcurrentHashMap<>();

    @SuppressWarnings("java:S1905") // Cast needed: IDE doesn't resolve Project→ComponentManager supertype
    public static ToolExecutionCorrelator getInstance(@NotNull Project project) {
        return ((com.intellij.openapi.components.ComponentManager) project).getService(ToolExecutionCorrelator.class);
    }

    /**
     * Execute a tool with optional synchronization and record the result for correlation.
     * 
     * @param toolName Normalized tool name (e.g., "git_status")
     * @param args Tool arguments
     * @param def Tool definition (provides execute method)
     * @param requiresSync If true, acquires per-tool lock before execution
     * @return Raw tool result
     * @throws Exception if tool execution fails
     */
    public String executeAndRecord(
        @NotNull String toolName,
        @NotNull JsonObject args,
        @NotNull ToolDefinition def,
        boolean requiresSync
    ) throws Exception {
        String argsHash = computeArgsHash(args);
        ReentrantLock lock = requiresSync ? toolLocks.computeIfAbsent(toolName, k -> new ReentrantLock()) : null;

        // Acquire sync lock if needed
        if (lock != null) {
            lock.lock();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Acquired sync lock for " + toolName + " (waiting threads: " + lock.getQueueLength() + ")");
            }
        }

        try {
            long startMs = System.currentTimeMillis();
            String result = def.execute(args);
            long durationMs = System.currentTimeMillis() - startMs;

            // Record execution for later correlation
            PendingExecution execution = new PendingExecution(
                toolName,
                args,
                argsHash,
                result,
                System.currentTimeMillis(),
                new AtomicBoolean(false)
            );

            ConcurrentLinkedQueue<PendingExecution> queue = pendingByTool.computeIfAbsent(
                toolName,
                k -> new ConcurrentLinkedQueue<>()
            );
            queue.offer(execution);

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format(
                    "Recorded execution: %s (hash=%s, duration=%dms, queueSize=%d, resultLen=%d)",
                    toolName, argsHash.substring(0, Math.min(8, argsHash.length())),
                    durationMs, queue.size(), result.length()
                ));
            }

            // Cleanup old entries periodically
            cleanupOldExecutions();

            return result;
        } finally {
            // Always release lock
            if (lock != null) {
                lock.unlock();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Released sync lock for " + toolName);
                }
            }
        }
    }

    /**
     * Find and consume the matching execution result for an agent tool update.
     * 
     * <p><b>Matching strategy (in order):</b>
     * <ol>
     *   <li><b>Exact match:</b> toolName + argsHash match within 30s window
     *       <br>→ Best for concurrent read-only tools with different args</li>
     *   <li><b>FIFO fallback:</b> First unconsumed execution for this tool within 30s
     *       <br>→ Works even if agent modified args or args unavailable</li>
     *   <li><b>No match:</b> Return null, caller should use agent's description as fallback</li>
     * </ol>
     * 
     * <p><b>Why FIFO works:</b> Sync tools execute one at a time, so queue order = execution order.
     * Even for concurrent tools, FIFO is reasonable if args don't match (agent might have tweaked them).
     * 
     * @param toolName Normalized tool name
     * @param args Tool arguments from agent update (may be null if unavailable)
     * @return Raw tool result if match found, null otherwise
     */
    @Nullable
    public String consumeResult(@NotNull String toolName, @Nullable JsonObject args) {
        ConcurrentLinkedQueue<PendingExecution> queue = pendingByTool.get(toolName);
        if (queue == null || queue.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No pending executions for " + toolName);
            }
            return null;
        }

        String argsHash = args != null ? computeArgsHash(args) : null;
        long cutoffTimestamp = System.currentTimeMillis() - MATCH_WINDOW_MS;

        // Strategy 1: Try exact match on args hash (best for concurrent tools)
        if (argsHash != null) {
            for (PendingExecution exec : queue) {
                if (exec.timestamp >= cutoffTimestamp
                    && exec.argsHash.equals(argsHash)
                    && exec.consumed.compareAndSet(false, true)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format(
                            "✓ Matched execution by args hash: %s (hash=%s, age=%dms)",
                            toolName, argsHash.substring(0, Math.min(8, argsHash.length())),
                            System.currentTimeMillis() - exec.timestamp
                        ));
                    }
                    return exec.result;
                }
            }
        }

        // Strategy 2: FIFO fallback - consume first unconsumed recent execution
        for (PendingExecution exec : queue) {
            if (exec.timestamp >= cutoffTimestamp && exec.consumed.compareAndSet(false, true)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format(
                        "⚠ Matched execution by FIFO fallback: %s (age=%dms, argsMatch=%s)",
                        toolName, System.currentTimeMillis() - exec.timestamp,
                        argsHash != null && exec.argsHash.equals(argsHash) ? "yes" : "no"
                    ));
                }
                return exec.result;
            }
        }

        // Strategy 3: No match - caller should use agent's description
        if (LOG.isDebugEnabled()) {
            int recentCount = 0;
            int consumedCount = 0;
            for (PendingExecution exec : queue) {
                if (exec.timestamp >= cutoffTimestamp) {
                    recentCount++;
                    if (exec.consumed.get()) consumedCount++;
                }
            }
            LOG.debug(String.format(
                "✗ No matching execution found for %s (recentInWindow=%d, alreadyConsumed=%d)",
                toolName, recentCount, consumedCount
            ));
        }
        return null;
    }

    /**
     * Determines if a tool requires synchronous execution (one at a time per tool name).
     * 
     * @param def Tool definition
     * @return true if tool should acquire sync lock before execution
     */
    public boolean requiresSync(@NotNull ToolDefinition def) {
        return SYNC_TOOL_CATEGORIES.contains(def.category().name());
    }

    /**
     * Computes a deterministic hash of tool arguments for matching.
     * Uses TreeMap to ensure consistent ordering regardless of insertion order.
     * 
     * @param args Tool arguments
     * @return Hex string hash (8 characters)
     */
    @NotNull
    private String computeArgsHash(@NotNull JsonObject args) {
        try {
            // TreeMap ensures stable ordering for consistent hashing
            Map<String, Object> sorted = new TreeMap<>();
            for (String key : args.keySet()) {
                sorted.put(key, args.get(key).toString());
            }
            String normalized = sorted.toString();
            return Integer.toHexString(normalized.hashCode());
        } catch (Exception e) {
            LOG.warn("Failed to compute args hash", e);
            return "00000000";
        }
    }

    /**
     * Removes executions older than RETENTION_MS to prevent unbounded memory growth.
     * Called periodically after each execution.
     */
    private void cleanupOldExecutions() {
        long cutoffTimestamp = System.currentTimeMillis() - RETENTION_MS;
        int removedTotal = 0;

        for (var entry : pendingByTool.entrySet()) {
            ConcurrentLinkedQueue<PendingExecution> queue = entry.getValue();
            int sizeBefore = queue.size();
            queue.removeIf(exec -> exec.timestamp < cutoffTimestamp);
            int removed = sizeBefore - queue.size();
            removedTotal += removed;
        }

        if (removedTotal > 0 && LOG.isDebugEnabled()) {
            LOG.debug("Cleaned up " + removedTotal + " old executions");
        }
    }

    /**
     * Clears all pending executions. Useful for testing or session reset.
     */
    public void clear() {
        int totalCleared = pendingByTool.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
        pendingByTool.clear();
        if (totalCleared > 0 && LOG.isDebugEnabled()) {
            LOG.debug("Cleared all pending executions (" + totalCleared + " entries)");
        }
    }
}

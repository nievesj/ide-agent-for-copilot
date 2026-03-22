package com.github.catatafishen.ideagentforcopilot.psi;

import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks cumulative lines added/removed by agent editing tools during a single turn.
 * Cleared at the start of each turn; read and reported at turn completion.
 */
public final class CodeChangeTracker {

    private static final AtomicInteger linesAdded = new AtomicInteger();
    private static final AtomicInteger linesRemoved = new AtomicInteger();

    private CodeChangeTracker() {
    }

    public static void recordChange(int added, int removed) {
        if (added > 0) linesAdded.addAndGet(added);
        if (removed > 0) linesRemoved.addAndGet(removed);
    }

    /** Returns [added, removed] and resets counters to zero. */
    public static int[] getAndClear() {
        return new int[]{linesAdded.getAndSet(0), linesRemoved.getAndSet(0)};
    }

    public static void clear() {
        linesAdded.set(0);
        linesRemoved.set(0);
    }

    /**
     * Computes exact lines added and removed between two content strings using IntelliJ's line diff.
     * Returns [added, removed].
     */
    public static int[] diffLines(String before, String after) {
        if (before.equals(after)) return new int[]{0, 0};
        String[] beforeLines = Diff.splitLines(before);
        String[] afterLines = Diff.splitLines(after);
        int added = 0, removed = 0;
        try {
            Diff.Change change = Diff.buildChanges(beforeLines, afterLines);
            for (Diff.Change c = change; c != null; c = c.link) {
                added += c.inserted;
                removed += c.deleted;
            }
        } catch (FilesTooBigForDiffException ignored) {
            // Fall back to simple line count difference for very large files
            added = afterLines.length;
            removed = beforeLines.length;
        }
        return new int[]{added, removed};
    }

    /** Counts lines in a string (number of newlines + 1, or 0 for empty). */
    public static int countLines(String content) {
        if (content == null || content.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') count++;
        }
        return count;
    }
}

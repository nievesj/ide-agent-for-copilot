package com.github.catatafishen.ideagentforcopilot.psi;

import com.intellij.openapi.application.ApplicationManager;

/**
 * Utility for dispatching operations to the EDT.
 * <p>
 * Tool handlers that need VFS/PSI access should ideally perform those reads on a
 * background thread (e.g. via ReadAction) and only hop to EDT for actual UI mutations.
 */
public final class EdtUtil {

    private EdtUtil() {
    }

    /**
     * Dispatch a runnable to the EDT.
     */
    public static void invokeLater(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    /**
     * Block the calling thread until the runnable completes on the EDT.
     */
    public static void invokeAndWait(Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(runnable);
    }
}

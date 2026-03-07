package com.github.catatafishen.ideagentforcopilot.bridge;

/**
 * Exception thrown when ACP (Agent Client Protocol) operations fail.
 */
public class AcpException extends Exception {
    private final boolean recoverable;

    public AcpException(String message) {
        this(message, null, true);
    }

    public AcpException(String message, Throwable cause) {
        this(message, cause, true);
    }

    public AcpException(String message, Throwable cause, boolean recoverable) {
        super(message, cause);
        this.recoverable = recoverable;
    }

    /**
     * Whether this error is recoverable (e.g., network timeout vs. invalid session).
     */
    public boolean isRecoverable() {
        return recoverable;
    }
}

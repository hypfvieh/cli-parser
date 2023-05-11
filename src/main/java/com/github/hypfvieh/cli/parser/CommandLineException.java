package com.github.hypfvieh.cli.parser;

/**
 * A run-time exception to report a command-line parsing error or failure.
 */
public class CommandLineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param _message the detail message
     */
    public CommandLineException(String _message) {
        super(_message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param _message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param _cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public CommandLineException(String _message, Throwable _cause) {
        super(_message, _cause);
    }
}

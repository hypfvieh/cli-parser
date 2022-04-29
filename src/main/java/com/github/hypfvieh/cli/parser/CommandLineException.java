package com.github.hypfvieh.cli.parser;

public class CommandLineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommandLineException(String _message) {
        super(_message);
    }

    public CommandLineException(String _message, Throwable _cause) {
        super(_message, _cause);
    }
}
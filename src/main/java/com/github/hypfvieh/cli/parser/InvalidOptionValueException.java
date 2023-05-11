package com.github.hypfvieh.cli.parser;

/**
 * A run-time exception to report a invalid option value on a command with a list of possible options.
 */
public class InvalidOptionValueException extends CommandLineException {

    private static final long serialVersionUID = 1L;

    public InvalidOptionValueException(String _message, Throwable _cause) {
        super(_message, _cause);
        // TODO Auto-generated constructor stub
    }

    public InvalidOptionValueException(String _message) {
        super(_message);
        // TODO Auto-generated constructor stub
    }

}

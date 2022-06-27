package com.github.hypfvieh.cli.parser;

import java.util.Objects;

/**
 * Utility bundling re-used static methods.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public final class StaticUtils {

    private StaticUtils() {
    }

    /**
     * Checks given option is valid (has valid name/shortname).
     *
     * @param _option option
     * @return option
     * @throws IllegalArgumentException when option is invalid
     */
    static CmdArgOption<?> requireOption(CmdArgOption<?> _option) {
        CmdArgOption<?> o = Objects.requireNonNull(_option, "Option required");
        if ((o.getName() == null || o.getName().isBlank()) && (o.getShortName() == null || o.getShortName().isBlank())) {
            throw new IllegalArgumentException("Command-line option requires a name or shortname: " + _option);
        }
        return o;
    }

    /**
     * Checks if the given option was not registered with same long/short name before.
     *
     * @param <B> command line
     * @param _option option to check
     * @param _cmdLine command line instance
     *
     * @throws IllegalArgumentException when option with same name already registered
     */
    static <B extends AbstractBaseCommandLine<?>> void requireUniqueOption(CmdArgOption<?> _option, B _cmdLine) {
        Objects.requireNonNull(_option, "Option required");
        Objects.requireNonNull(_cmdLine, "Commandline required");

        if (_option.getName() != null && _cmdLine.getOptions().containsKey(_option.getName())) {
            throw new IllegalArgumentException("Command-line option '" + _cmdLine.getLongOptPrefix() + _option.getName() + "' already defined");
        }

        if (_option.getShortName() != null && _cmdLine.getOptions().containsKey(_option.getShortName())) {
            throw new IllegalArgumentException("Command-line option '" + _cmdLine.getShortOptPrefix() + _option.getShortName() + "' already defined");
        }
    }

    /**
     * Creates a new exception instance of the given type.
     *
     * @param _message message for exception
     * @param _exceptionType exception class to instantiate
     *
     * @return instance of RuntimeException compatible class, maybe {@link CommandLineException} in case given exception
     *         does not support String constructor
     */
    static RuntimeException createException(String _message, Class<? extends RuntimeException> _exceptionType) {
        if (_exceptionType == null) {
            return new RuntimeException(_message);
        }
        if (CommandLineException.class == _exceptionType) {
            return new CommandLineException(_message);
        }
        try {
            return _exceptionType.getConstructor(String.class).newInstance(_message);
        } catch (Exception _ex) {
            return new CommandLineException(_message);
        }
    }

    /**
     * Verifies that the parse was called on the given commandline instance.
     *
     * @param <B> command line type
     * @param _cmdLine command line instance
     *
     * @return command line
     * @throws RuntimeException (or subclass) when command line was not parsed
     */
    static <B extends AbstractBaseCommandLine<?>> B requireParsed(B _cmdLine) {
        Objects.requireNonNull(_cmdLine, "CommandLine required");
        if (!_cmdLine.isParsed()) {
            throw createException("Command-line not parsed", _cmdLine.getExceptionType());
        }
        return _cmdLine;
    }

    /**
     * Executes an unchecked cast on the given type.
     *
     * @param <T> type
     * @param _type type
     * @return class
     */
    @SuppressWarnings("unchecked")
    static <T> Class<T> uncheckedCast(Class<?> _type) {
        return (Class<T>) _type;
    }

    /**
     * Creates a "Option not defined" exception.
     *
     * @param _option option which was not defined
     * @return CommandLineException
     */
    static <T extends RuntimeException> RuntimeException optionNotDefined(Object _option, Class<T> _targetClass) {
        return createException("Option not defined: " + _option, _targetClass);
    }

    /**
     * Checks if given string was null or blank.
     *
     * @param _val input to check
     * @return input string or null
     */
    static String trimToNull(String _val) {
        if (_val == null || _val.isBlank()) {
            return null;
        }
        return _val;
    }

    /**
     * Formats the given option for logging/exceptions.
     *
     * @param _arg argument to convert
     * @param _longOptPrefix prefix for long options
     * @param _shortOptPrefix prefix for short options
     *
     * @return String or null if option was null
     */
    public static String formatOption(CmdArgOption<?> _arg, String _longOptPrefix, String _shortOptPrefix) {
        if (_arg == null) {
            return null;
        } else if (_arg.getName() != null && !_arg.getName().isBlank() && _arg.getShortName() != null && !_arg.getShortName().isBlank()) {
            return _longOptPrefix + _arg.getName() + "/" + _shortOptPrefix + _arg.getShortName();
        } else if (_arg.getName() != null && !_arg.getName().isBlank()) {
            return _longOptPrefix + _arg.getName();
        } else if (_arg.getShortName() != null && !_arg.getShortName().isBlank()) {
            return _shortOptPrefix + _arg.getShortName();
        } else {
            return "?";
        }
    }
}

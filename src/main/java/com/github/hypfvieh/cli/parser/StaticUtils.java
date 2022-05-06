package com.github.hypfvieh.cli.parser;

import java.util.Objects;

/**
 * Utility bundling re-used static methods.
 * 
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public final class StaticUtils {

    private StaticUtils() {}
    
    static CmdArgOption<?> requireOption(CmdArgOption<?> _option) {
        CmdArgOption<?> o = Objects.requireNonNull(_option, "Option required");
        if ((o.getName() == null || o.getName().isBlank()) && (o.getShortName() == null || o.getShortName().isBlank())) {
            throw new IllegalArgumentException("Command-line option requires a name or shortname: " + _option);
        }
        return o;
    }

    static <B extends AbstractBaseCommandLine<?>> void requireUniqueOption(CmdArgOption<?> _option, B _c) {
        Objects.requireNonNull(_option, "Option required");
        Objects.requireNonNull(_c, "Commandline required");
        
        if (_option.getName() != null && _c.getOptions().containsKey(_option.getName())) {
            throw new IllegalArgumentException("Command-line option '" + _c.getLongOptPrefix() + _option.getName() + "' already defined");
        }
        
        if (_option.getShortName() != null && _c.getOptions().containsKey(_option.getShortName())) {
            throw new IllegalArgumentException("Command-line option '" + _c.getShortOptPrefix() + _option.getShortName() + "' already defined");
        }
    }

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
    
    static <B extends AbstractBaseCommandLine<?>> B requireParsed(B _c) {
        Objects.requireNonNull(_c, "CommandLine required");
        if (!_c.isParsed()) {
            throw createException("Command-line not parsed", _c.getExceptionType());
        }
        return _c;
    }
 
    @SuppressWarnings("unchecked")
    static <T> Class<T> uncheckedCast(Class<?> _type) {
        return (Class<T>) _type;
    }
    
    static CommandLineException optionNotDefined(Object _option) {
        return new CommandLineException("Option not defined: " + _option);
    }
    
    static String trimToNull(String _val) {
        if (_val == null || _val.isBlank()) {
            return null;
        }
        return _val;
    }

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

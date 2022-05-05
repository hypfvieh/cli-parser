package com.github.hypfvieh.cli.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility bundling re-used static methods.
 * 
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public final class StaticUtils {

    static <T> T requireOption(T _option) {
        T o = Objects.requireNonNull(_option, "Option required");
        if (o instanceof CmdArgOption<?> opt) {
            if ((opt.getName() == null || opt.getName().isBlank()) && (opt.getShortName() == null || opt.getShortName().isBlank())) {
                throw new IllegalArgumentException("Command-line option requires a name or shortname: " + _option);
            }
        }
        return o;
    }

    static <B extends AbstractBaseCommandLine<?>> void requireUniqueOption(CmdArgOption<?> _option, B _c) {
        if (_c == null) {
            return;
        }
        if (_option.getName() != null && _c.getOptions().containsKey(_option.getName())) {
            throw new IllegalArgumentException("Command-line option '" + _c.getLongOptPrefix() + _option.getName() + "' already defined");
        }
        
        if (_option.getShortName() != null && _c.getOptions().containsKey(_option.getShortName())) {
            throw new IllegalArgumentException("Command-line option '" + _c.getShortOptPrefix() + _option.getShortName() + "' already defined");
        }
    }

    static RuntimeException createException(String _message, Class<? extends RuntimeException> _exceptionType) {
        if (CommandLineException.class.isInstance(_exceptionType)) {
            return new CommandLineException(_message);
        }
        try {
            return _exceptionType.getConstructor(String.class).newInstance(_message);
        } catch (Exception _ex) {
            return new CommandLineException(_message);
        }
    }
    
    static <B extends AbstractBaseCommandLine<?>> B requireParsed(B _c) {
        if (!_c.isParsed()) {
            throw createException("Command-line not parsed", _c.getExceptionType());
        }
        return _c;
    }
 
    @SuppressWarnings("unchecked")
    static <T> Class<T> uncheckedCast(Class<?> _type) {
        return (Class<T>) _type;
    }
    
    static String printableArgName(CmdArgOption<?> _opt) {
        List<String> k = new ArrayList<>();
        if (_opt.getName() != null) {
            k.add("--" + _opt.getName());
        } 
        if (_opt.getShortName() != null) {
            k.add("-" + _opt.getShortName());
        }
        
        return String.join("/", k);
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

    static <B extends AbstractBaseCommandLine<?>> String formatOption(CmdArgOption<?> _arg, B _c) {
        if (_arg == null) {
            return null;
        } else if (_arg.getName() != null && !_arg.getName().isBlank() && _arg.getShortName() != null && !_arg.getShortName().isBlank()) {
            return _c.getLongOptPrefix() + _arg.getName() + "/" + _c.getShortOptPrefix() + _arg.getShortName();
        } else if (_arg.getName() != null && !_arg.getName().isBlank()) {
            return _c.getLongOptPrefix() + _arg.getName();
        } else if (_arg.getShortName() != null && !_arg.getShortName().isBlank()) {
            return _c.getShortOptPrefix() + _arg.getShortName();   
        } else {
            return "?";
        }
    }
}

package com.github.hypfvieh.cli.parser;

/**
 * Class which represents a parsed command line argument.
 * 
 * @version 1.0.0 - 2022-05-05
 */
public class ParsedArg {

    /** True if argument looks like a short or long option. */ 
    private final boolean lookingLikeOption;
    /** True if this is a short-option repeated multiple times (e.g. -vvv). */
    private final boolean multiArg;
    /** The command option, if any. */
    private final CmdArgOption<?> cmdArgOpt;

    public ParsedArg(boolean _looksLikeArg, boolean _multi, CmdArgOption<?> _cmdArg) {
        lookingLikeOption = _looksLikeArg;
        multiArg = _multi;
        cmdArgOpt = _cmdArg;
    }

    public boolean isLookingLikeOption() {
        return lookingLikeOption;
    }

    public boolean isMultiArg() {
        return multiArg;
    }

    public CmdArgOption<?> getCmdArgOpt() {
        return cmdArgOpt;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [lookingLikeOption=" + lookingLikeOption + ", multiArg=" + multiArg + ", cmdArgOpt="
                + cmdArgOpt + "]";
    }

}

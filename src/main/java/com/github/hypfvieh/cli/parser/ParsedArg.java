package com.github.hypfvieh.cli.parser;

/**
 * Class which represents a parsed command line argument.
 * 
 * @since 1.0.0 - 2022-05-05
 */
class ParsedArg {

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

    /**
     * Indicates that the parsed token looks like a long or short option.
     * @return true if option like
     */
    public boolean isLookingLikeOption() {
        return lookingLikeOption;
    }

    /**
     * True if the parsed token looks like a repeated argument.
     * @return true if repeated
     */
    public boolean isMultiArg() {
        return multiArg;
    }

    /**
     * Option used in combination with the parsed token.
     * @return option, maybe null
     */
    public CmdArgOption<?> getCmdArgOpt() {
        return cmdArgOpt;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [lookingLikeOption=" + lookingLikeOption + ", multiArg=" + multiArg + ", cmdArgOpt="
                + cmdArgOpt + "]";
    }

}

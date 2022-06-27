package com.github.hypfvieh.cli.parser;

/**
 * Class which represents a parsed command line argument.
 *
 * @since 1.0.0 - 2022-05-05
 */
class ParsedArg {

    /** True if argument looks like a short or long option. */
    private final boolean         lookingLikeOption;
    /** True if this is a short-option repeated multiple times (e.g. -vvv). */
    private final boolean         multiArg;
    /** The command option, if any. */
    private final CmdArgOption<?> cmdArgOpt;

    private String                value;

    ParsedArg(boolean _looksLikeArg, boolean _multi, CmdArgOption<?> _cmdArg) {
        this(_looksLikeArg, _multi, _cmdArg, null);
    }

    ParsedArg(boolean _looksLikeArg, boolean _multi, CmdArgOption<?> _cmdArg, String _value) {
        lookingLikeOption = _looksLikeArg;
        multiArg = _multi;
        cmdArgOpt = _cmdArg;
        value = _value;
    }

    /**
     * Indicates that the parsed token looks like a long or short option.
     *
     * @return true if option like
     */
    public boolean isLookingLikeOption() {
        return lookingLikeOption;
    }

    /**
     * True if the parsed token looks like a repeated argument.
     *
     * @return true if repeated
     */
    public boolean isMultiArg() {
        return multiArg;
    }

    /**
     * Option used in combination with the parsed token.
     *
     * @return option, maybe null
     */
    public CmdArgOption<?> getCmdArgOpt() {
        return cmdArgOpt;
    }

    /**
     * The value assigned to the parsed option.<br>
     * Can be null if option value was not given.
     *
     * @return String or null
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the current value for the parsed option.
     *
     * @param _value value to set
     */
    public void setValue(String _value) {
        value = _value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [lookingLikeOption=" + lookingLikeOption + ", multiArg=" + multiArg + ", cmdArgOpt="
                + cmdArgOpt + ", value=" + value + "]";
    }

}

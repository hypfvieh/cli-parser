package com.github.hypfvieh.cli.parser;

import static com.github.hypfvieh.cli.parser.StaticUtils.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * A class to parse the Java command-line and access arguments by name and type using {@link CmdArgOption}s.
 * <p>
 *
 * Example of working with a command-line with two options and a custom exception type:
 *
 * <pre>
 * CommandLine commandLine = new CommandLine()
 *         .setExceptionType(MyApplicationException.class)
 *         .addOption(Option.builder(String.class)
 *                 .name("req1")
 *                 .required(true)
 *                 .defaultValue("default")
 *                 .build())
 *         .addOption(Option.builder()
 *                 .name("opt1")
 *                 .required(false)
 *                 .build())
 *         .parse();
 * </pre>
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-04-29
 */
public final class CommandLine extends AbstractBaseCommandLine<CommandLine> {

    /**
     * Reference to ourselves for chaining.
     */
    @Override
    protected CommandLine self() {
        return this;
    }

    /**
     * Parses a String as commandline by splitting it using space as delimiter.
     * Only intended to be used for testing.
     *
     * @param _args String
     * @return this
     */
    CommandLine parse(String _args) {
        return parse(_args == null ? null : _args.split(" "));
    }

    /**
     * Parses the given arguments.
     *
     * @param _args arguments to read
     *
     * @return this
     */
    public synchronized CommandLine parse(String[] _args) {
        getLogger().debug("Parsing command-line: {}", Arrays.toString(_args));

        reset();

        if (_args != null) {
            final int argsLen = _args.length;

            for (int i = 0; i < argsLen; i++) {
                getLogger().trace("Token {}/{}: {}", i, argsLen, _args[i]);

                String token = Optional.ofNullable(_args[i]).map(String::trim).orElse("");

                if (token.isEmpty()) {
                    continue;
                }

                ParsedArg parsedArg = parseArg(token, true);
                CmdArgOption<?> cmdOpt = parsedArg.getCmdArgOpt();

                if (argsLen - 1 >= i + 1) {
                    String val = _args[i + 1];
                    ParsedArg nextArg = parseArg(val, false);

                    if (nextArg.getCmdArgOpt() == null) { // looks like proper value
                        if (cmdOpt != null) {
                            handleCmdOption(cmdOpt, val);
                            i++;
                        } else if (parsedArg.isLookingLikeOption()) {
                            getArgBundle().getUnknownArgs().put(token, val);
                            i++;
                        } else {
                            getArgBundle().getUnknownTokens().add(token);
                        }
                    } else { // next token is an option too
                        if (cmdOpt != null) {
                            if (cmdOpt.hasValue() && parsedArg.getValue() == null) { // command needs option, but got another option
                                getArgBundle().getMissingArgs().add(cmdOpt);
                            } else if (!cmdOpt.isRepeatable()) { // no arguments required for option
                                handleCmdOption(cmdOpt, parsedArg.getValue());
                            }
                        } else {
                            getArgBundle().getUnknownTokens().add(token);
                        }
                    }
                } else { // no arguments left

                    if (cmdOpt != null) {
                        if (cmdOpt.hasValue() && parsedArg.getValue() == null) { // option needs value but we already on the last token
                            getArgBundle().getMissingArgs().add(cmdOpt);
                        } else if (cmdOpt.hasValue() && parsedArg.getValue() != null) { // options value was given using -o=val
                            handleCmdOption(cmdOpt, parsedArg.getValue());
                        } else if (!parsedArg.isMultiArg()) { // not a repeated option)
                            handleCmdOption(cmdOpt, parsedArg.getValue());
                        }
                    } else if (!parsedArg.isLookingLikeOption()) { // we on last token and this does not look like an option
                        getArgBundle().getUnknownTokens().add(token);
                    } else if (cmdOpt == null && parsedArg.isLookingLikeOption()) { // we did not find an option but this argument looks like one
                        getArgBundle().getUnknownArgs().put(token, null);
                    }
                }
            }
        }

        setParsed(true);

        logResults();
        validate();

        return self();
    }

    /**
     * Returns the value associated with argument option.
     * <p>
     * If no value is present, the default value of that option is returned (and might by <code>null</code>). <br>
     * If the option does not support values or option was not set, null is returned.<br>
     * If the option is a repeatable option, the value of the first occurrence is returned.
     * </p>
     *
     * @param <T> type of option value
     * @param _option option
     *
     * @return value, maybe <code>null</code>
     */
    public <T> T getArg(CmdArgOption<T> _option) {
        return getArg(_option, null);
    }

    /**
     * Returns the value associated with argument option.
     * <p>
     * If no value is present, the given default value is used.<br>
     * If the given default is also <code>null</code>, the default of that option is returned (and might by <code>null</code>).
     * If the option does not support values or if the option was not set, <code>null</code> is returned.<br>
     * </p>
     *
     * @param <T> type of option value
     * @param _option option
     * @param _default default to use when no value present (overrides default specified in option)
     *
     * @return value, maybe <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public <T> T getArg(CmdArgOption<T> _option, T _default) {
        List<T> args = getArgs(_option, _default);
        if (args == null || args.isEmpty()) {
            return null;
        }
        T convertedVal = args.get(0);
        return _option.getDataType().isPrimitive() ? convertedVal : (T) _option.getDataType().cast(convertedVal);
    }

    /**
     * Returns the value associated with argument option.
     * <p>
     * If no value is present, the default value of that option is returned (and might by <code>null</code>).
     * If the option does not support values or if the option was not set, <code>null</code> is returned.<br>
     * </p>
     *
     * @param <T> type of option value
     * @param _option option
     *
     * @return List, maybe empty or <code>null</code>
     */
    public <T> List<T> getArgs(CmdArgOption<T> _option) {
        return getArgs(_option, null);
    }

    /**
     * Returns the value associated with argument option.
     * <p>
     * If no value is present, the given default value is used.<br>
     * If the given default is also <code>null</code>, the default of that option is returned (and might by <code>null</code>).
     * If the option does not support values or if the option was not set, <code>null</code> is returned.<br>
     * </p>
     *
     * @param <T> type of option value
     * @param _option option
     * @param _default default to use when no value present (overrides default specified in option)
     *
     * @return List, maybe empty or <code>null</code>
     */
    public <T> List<T> getArgs(CmdArgOption<T> _option, T _default) {
        Objects.requireNonNull(_option, "Option required");
        List<String> strVals = new ArrayList<>();

        if (_option.isRepeatable()) {
            List<String> list = getArgBundle().getKnownMultiArgs().get(_option);
            if (list != null && !list.isEmpty()) {
                strVals.addAll(list);
            }
        } else {
            String val = getArgBundle().getKnownArgs().get(_option);
            if (val != null) {
                strVals.add(val);
            }
        }

        if (_option.hasValue()) {
            return convertValues(_option, _default, strVals);
        }

        return null;
    }

    /**
     * Converts the value of the given option using configured converters or returns default.
     *
     * @param <T> type
     *
     * @param _option option
     * @param _default default if option not set
     * @param _strVals string values
     *
     * @return value or null
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> convertValues(CmdArgOption<T> _option, T _default, List<String> _strVals) {
        List<T> resultList = new ArrayList<>();
        if (_strVals.isEmpty()) {
            if (_default == null) {
                var x = (T) _option.getDefaultValue();
                if (x != null) {
                    resultList.add(x);
                }
            } else {
                resultList.add(_default);
            }
            return resultList;
        }

        for (String str : _strVals) {
            T convertedVal = (T) getArgBundle().getConverters().get(_option.getDataType()).convert(str);
            resultList.add(convertedVal);
        }
        return resultList;
    }

    /**
     * Returns an option value using the options name.
     *
     * @param _optionName option name
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed before
     */
    public Object getArg(CharSequence _optionName) {
        return Optional.ofNullable(requireParsed(this).getOption(_optionName))
                .map(this::getArg)
                .orElseThrow(() -> optionNotDefined(_optionName, getExceptionType()));
    }

    /**
     * Returns an option value using the options short name.
     *
     * @param _optionName option short name
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed before
     */
    public Object getArg(char _optionName) {
        return Optional.ofNullable(requireParsed(this).getOption(_optionName + ""))
                .map(this::getArg)
                .orElseThrow(() -> optionNotDefined(_optionName, getExceptionType()));
    }

    /**
     * Returns an option value using the options name and converting the value to the given type.
     * <br>
     * Will use the configured converter to convert the value.<br><br>
     *
     * If given type is not the same as the type specified in {@link CmdArgOption} an exception is thrown.
     *
     * @param _optionName option short name
     * @param _type expected value type
     * @param _default default to use if option not set (and not required)
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed<br>
     * @throws RuntimeException when type class is not the type defined in {@link CmdArgOption}
     *
     * @since 1.0.1 - 2022-05-24
     */
    public <T> T getArg(CharSequence _optionName, Class<T> _type, T _default) {
        requireParsed(this);
        CmdArgOption<?> option = getOption(_optionName);

        if (_type != option.getDataType()) {
            throw createException("Invalid type conversation, expected: " + option.getDataType().getName() + " - found: " + _type.getName(), getExceptionType());
        }

        Object arg = getArg(option);
        if (arg == null || arg.equals(option.getDefaultValue())) { // no value or default of CmdArgOption -> override with our default
            return _default;
        }

        return _type.cast(arg);
    }

    /**
     * Returns an option value using the options name and converting the value to the given type.
     * <br>
     * Will use the configured converter to convert the value.<br><br>
     *
     * If given type is not the same as the type specified in {@link CmdArgOption} an exception is thrown.
     *
     * @param _optionName option short name
     * @param _default default to use if option not set (and not required)
     * @param _type expected value type
     *
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed<br>
     * @throws RuntimeException when type class is not the type defined in {@link CmdArgOption}
     *
     * @since 1.0.1 - 2022-05-24
     */
    public <T> T getArg(CharSequence _optionName, Class<T> _type) {
        requireParsed(this);
        CmdArgOption<?> option = getOption(_optionName);
        if (_type != option.getDataType()) {
            throw createException("Invalid type conversation, expected: " + option.getDataType().getName() + " - found: " + _type.getName(), getExceptionType());
        }

        return _type.cast(getArg(option));
    }

    /**
     * Returns an option value using the options short name and converting the value to the given type.
     * <br>
     * Will use the configured converter to convert the value.<br><br>
     *
     * If given type is not the same as the type specified in {@link CmdArgOption} an exception is thrown.
     *
     * @param _optionName option short name
     * @param _type expected value type
     *
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed<br>
     * @throws RuntimeException when type class is not the type defined in {@link CmdArgOption}
     *
     * @since 1.0.1 - 2022-05-24
     */
    public <T> T getArg(char _optionName, Class<T> _type) {
        return getArg(_optionName + "", _type);
    }

    /**
     * Returns an option value using the options short name and converting the value to the given type.
     * <br>
     * Will use the configured converter to convert the value.<br><br>
     *
     * If given type is not the same as the type specified in {@link CmdArgOption} an exception is thrown.
     *
     * @param _optionName option short name
     * @param _type expected value type
     * @param _default default to use if option not set (and not required)
     *
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed<br>
     * @throws RuntimeException when type class is not the type defined in {@link CmdArgOption}
     *
     * @since 1.0.1 - 2022-05-24
     */
    public <T> T getArg(char _optionName, Class<T> _type, T _default) {
        return getArg(_optionName + "", _type, _default);
    }

    /**
     * Returns all option values using the options name and converting the values to the given type.
     * <br>
     * Will use the configured converter to convert the value.<br><br>
     *
     * If given type is not the same as the type specified in {@link CmdArgOption} an exception is thrown.
     *
     * @param _optionName option short name
     * @param _type expected value type
     *
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed<br>
     * @throws RuntimeException when type class is not the type defined in {@link CmdArgOption}
     *
     * @since 1.0.1 - 2022-05-24
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getArgs(CharSequence _optionName, Class<T> _type) {
        requireParsed(this);
        CmdArgOption<?> option = getOption(_optionName);
        if (_type != option.getDataType()) {
            throw createException("Invalid type conversation, expected: " + option.getDataType().getName() + " - found: " + _type.getName(), getExceptionType());
        }

        return (List<T>) getArgs(option);
    }

    /**
     * Returns all option values using the options short name and converting the values to the given type.
     * <br>
     * Will use the configured converter to convert the value.<br><br>
     *
     * If given type is not the same as the type specified in {@link CmdArgOption} an exception is thrown.
     *
     * @param _optionName option short name
     * @param _type expected value type
     *
     * @return value or null if option has no value
     *
     * @throws RuntimeException when option is unknown or command line was not parsed<br>
     * @throws RuntimeException when type class is not the type defined in {@link CmdArgOption}
     *
     * @since 1.0.1 - 2022-05-24
     */
    public <T> List<T> getArgs(char _optionName, Class<T> _type) {
        return getArgs(_optionName + "", _type);
    }

    /**
     * Checks if the given option was at least used once in the command line.
     *
     * @param _option option to check
     * @return true if it was used at least once, false otherwise
     */
    public boolean hasArg(CmdArgOption<?> _option) {
        Objects.requireNonNull(_option, "Option required");
        CommandLine requireParsed = requireParsed(this);
        if (getArgBundle().getKnownArgs().containsKey(requireParsed.getArgBundle().getOptions().get(requireOption(_option).getName()))) {
            return true;
        } else if (getArgBundle().getKnownMultiArgs().containsKey(requireParsed.getArgBundle().getOptions().get(requireOption(_option).getName()))) {
            return true;
        }

        return Optional.ofNullable(requireParsed.getArgBundle().getOptions().get(requireOption(_option).getShortName()))
                .map(k -> getArgBundle().getKnownArgs().containsKey(k) || getArgBundle().getKnownMultiArgs().containsKey(k))
                .orElseThrow(() -> optionNotDefined(_option, getExceptionType()));
    }

    /**
     * Returns the number of occurrences of the given option.
     * <p>
     * If the option was never set, 0 is returned.
     * </p>
     *
     * @param _option option
     *
     * @return number of occurrences
     */
    public int getArgCount(CmdArgOption<?> _option) {
        Objects.requireNonNull(_option, "Option required");
        requireParsed(this);

        if (getArgBundle().getKnownArgs().containsKey(_option)) {
            return 1;
        }
        if (getArgBundle().getKnownMultiArgs().containsKey(_option)) {
            return getArgBundle().getKnownMultiArgs().get(_option).size();
        }

        return 0;
    }

    /**
     * Adds the given command option to the appropriate internal map or list.
     *
     * @param _cmdOpt option
     * @param _val value
     */
    private void handleCmdOption(CmdArgOption<?> _cmdOpt, String _val) {
        Objects.requireNonNull(_cmdOpt, "Option required");
        if (_cmdOpt.isRepeatable()) {
            getArgBundle().getKnownMultiArgs().computeIfAbsent(_cmdOpt, x -> new ArrayList<>()).add(trimToNull(_val));
        } else if (!getArgBundle().getKnownArgs().containsKey(_cmdOpt)) {
            getArgBundle().getKnownArgs().put(_cmdOpt, trimToNull(_val));
        } else {
            getArgBundle().getDupArgs().put(_cmdOpt, _val);
        }
    }

    /**
     * Reads a token using the long/short name regular expressions.
     *
     * @param _token token to read
     * @param _handle true to put detected arguments to internal map, false to do nothing
     *
     * @return ParsedArg - never null
     */
    private ParsedArg parseArg(String _token, boolean _handle) {
        if (_token == null) {
            return new ParsedArg(false, false, null);
        }

        String optionName = null;

        Matcher matcher = getLongOptPattern().matcher(_token);
        if (matcher.matches()) {
            optionName = matcher.group(1) == null ? matcher.group(3) : matcher.group(1);
            ParsedArg parsedArg = new ParsedArg(true, false, getArgBundle().getOptions().get(optionName));
            parsedArg.setValue(matcher.group(2));

            return parsedArg;
        } else {
            matcher = getShortOptPattern().matcher(_token);

            if (matcher.matches()) {

                String value = matcher.group(2);
                optionName = matcher.group(1) == null ? matcher.group(3) : matcher.group(1);

                if (optionName.length() > 1) {
                    CmdArgOption<?> cmdArgOption = null;
                    CmdArgOption<?> prevOption = null;
                    // iterate all combined short options (e.g. -abcd)
                    for (char c : optionName.toCharArray()) {
                        String key = c + "";
                        cmdArgOption = getArgBundle().getOptions().get(key);
                        if (cmdArgOption != null) {
                            if (!cmdArgOption.hasValue() && _handle) {
                                handleCmdOption(cmdArgOption, null);
                            } else if (prevOption == null && cmdArgOption.hasValue()) { // no option was value before
                                prevOption = cmdArgOption;
                            } else if (prevOption != null && prevOption.hasValue() && cmdArgOption.hasValue()) { // we already have an option which requires a value
                                throw createException("Option " + formatOption(prevOption, getLongOptPrefix(), getShortOptPrefix()) + " requires a value", getExceptionType());
                            }

                        } else {
                            if (_handle) { // got a unknown short option
                                getArgBundle().getUnknownArgs().put(key, null);
                            }
                        }
                    }
                    return new ParsedArg(true, true, cmdArgOption, value);
                } else {
                    CmdArgOption<?> cmdArgOption = getArgBundle().getOptions().get(optionName);
                    if (cmdArgOption == null) { // unknown argument used
                        return new ParsedArg(true, false, null, value);
                    }

                    return new ParsedArg(true, false, cmdArgOption, value);
                }
            }
        }
        return new ParsedArg(false, false, null);
    }

    /**
     * Validates the parsed command line.
     *
     * @return this
     *
     * @throws CommandLineException when validation fails
     */
    private CommandLine validate() throws CommandLineException {
        List<String> failures = new ArrayList<>();
        if (isFailOnUnknownArg() && !getArgBundle().getUnknownArgs().isEmpty()) {
            failures.add("unknown arguments: " + getArgBundle().getUnknownArgs().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        }
        if (isFailOnUnknownToken() && !getArgBundle().getUnknownTokens().isEmpty()) {
            failures.add("unknown tokens: " + String.join(", ", getArgBundle().getUnknownTokens()));
        }
        if (isFailOnDupArg() && !getArgBundle().getDupArgs().isEmpty()) {
            failures.add("duplicate arguments: " + getArgBundle().getDupArgs().keySet().stream().map(x -> formatOption(x, getLongOptPrefix(), getShortOptPrefix())).collect(Collectors.joining(", ")));
        }

        // check all required options are given
        List<String> missingOptions = getOptions().values().stream().filter(CmdArgOption::isRequired)
                .filter(s -> !getArgBundle().getKnownArgs().containsKey(s))
                .map(CmdArgOption::getName)
                .collect(Collectors.toList());
        if (!missingOptions.isEmpty()) {
            failures.add("required options missing: " + String.join(", ", missingOptions));
        }

        // check values
        for (Entry<CmdArgOption<?>, String> knownArg : getArgBundle().getKnownArgs().entrySet()) {
            CmdArgOption<?> option = knownArg.getKey();
            if (option.hasValue() && knownArg.getValue() == null && option.getDefaultValue() == null) {
                failures.add("argument '" + formatOption(knownArg.getKey(), getLongOptPrefix(), getShortOptPrefix()) + "' requires a value");
            } else if (!option.hasValue() && knownArg.getValue() != null) {
                failures.add("argument '" + formatOption(knownArg.getKey(), getLongOptPrefix(), getShortOptPrefix()) + "' cannot have a value");
            }
            // check value type
            if (option.hasValue() && knownArg.getValue() != null) {
                try {
                    getArg(option);
                } catch (Exception _ex) {
                    failures.add("argument '" + formatOption(knownArg.getKey(), getLongOptPrefix(), getShortOptPrefix()) + "' has invalid value ("
                            + getArgBundle().getKnownArgs().get(knownArg.getKey()) + ")");
                }
            }
        }

        if (!failures.isEmpty()) {
            throw createException("Parsing of command-line failed: " + String.join(", ", failures), getExceptionType());
        }
        return this;
    }

}

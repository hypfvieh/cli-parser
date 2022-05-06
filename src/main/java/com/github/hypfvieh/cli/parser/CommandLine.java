package com.github.hypfvieh.cli.parser;

import static com.github.hypfvieh.cli.parser.StaticUtils.createException;
import static com.github.hypfvieh.cli.parser.StaticUtils.formatOption;
import static com.github.hypfvieh.cli.parser.StaticUtils.optionNotDefined;
import static com.github.hypfvieh.cli.parser.StaticUtils.requireOption;
import static com.github.hypfvieh.cli.parser.StaticUtils.requireParsed;
import static com.github.hypfvieh.cli.parser.StaticUtils.trimToNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
 * @since 1.0.0 - 2022-04-29
 */
public final class CommandLine extends AbstractBaseCommandLine<CommandLine> {
    
    protected CommandLine self() {
        return this;
    }
    
    CommandLine parse(String _args) {
        return parse(_args == null ? null : _args.split(" "));
    }

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
                
                if (argsLen -1 >= i+1) {
                    String val = _args[i+1];
                    ParsedArg nextArg = parseArg(val, false);
                    
                    if (nextArg.getCmdArgOpt() == null) { // looks like proper value
                        if (cmdOpt != null) {
                            handleCmdOption(cmdOpt, val);
                            i++;
                        } else if (parsedArg.isLookingLikeOption()) {
                            getArgBundle().unknownArgs().put(token, val);
                            i++;
                        } else {
                            getArgBundle().unknownTokens().add(token);
                        }
                    } else { // next token is an option too
                        if (cmdOpt != null) {
                            if (cmdOpt.hasValue()) { // command needs option, but got another option
                                getArgBundle().missingArgs().add(cmdOpt);
                            } else if (!cmdOpt.isRepeatable()) { // no arguments required for option
                                handleCmdOption(cmdOpt, null);
                            }
                        } else {
                            getArgBundle().unknownTokens().add(token);
                        }
                    }
                } else { // no arguments left
                    if (cmdOpt != null && cmdOpt.hasValue()) { // option needs value but we already on the last token 
                        getArgBundle().missingArgs().add(cmdOpt);
                    } else if (!parsedArg.isLookingLikeOption()) { // we on last token and this does not look like an option
                        getArgBundle().unknownTokens().add(token);
                    } else if (cmdOpt == null && parsedArg.isLookingLikeOption()) { // we did not find an option but this argument looks like one
                        getArgBundle().unknownArgs().put(token, null);
                    } else if (cmdOpt != null && !parsedArg.isMultiArg()) { // not a repeated option 
                        handleCmdOption(cmdOpt, null);
                    }
                }
            }
        }

        setParsed(true);

        logResults();
        validate();

        return this;
    }


    private void handleCmdOption(CmdArgOption<?> _cmdOpt, String _val) {
        if (_cmdOpt.isRepeatable()) {
            getArgBundle().knownMultiArgs().computeIfAbsent(_cmdOpt, x -> new ArrayList<>()).add(trimToNull(_val));
        } else if (!_cmdOpt.isRepeatable() && !getArgBundle().knownArgs().containsKey(_cmdOpt)) {
            getArgBundle().knownArgs().put(_cmdOpt, trimToNull(_val));
        } else {
            getArgBundle().dupArgs().put(_cmdOpt, _val);
        }
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
    @SuppressWarnings("unchecked")
    public <T> T getArg(CmdArgOption<T> _option) {
        List<T> args = getArgs(_option);
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
    @SuppressWarnings("unchecked")
    public <T> List<T> getArgs(CmdArgOption<T> _option) {
        Objects.requireNonNull(_option, "Option required");
        List<String> strVals = new ArrayList<>();
        
        if (_option.isRepeatable()) {
            List<String> list = getArgBundle().knownMultiArgs().get(_option);
            if (list != null && !list.isEmpty()) {
                strVals.addAll(list);
            }
        } else {
            String val = getArgBundle().knownArgs().get(_option);
            if (val != null) {
                strVals.add(val);
            }
        }
        
        if (_option.hasValue()) {
            List<T> resultList = new ArrayList<>();
            if (strVals.isEmpty()) {
                var x = (T) _option.getDefaultValue();
                if (x != null) {
                    resultList.add(x);
                }
                return resultList;
            }
            
            for (String str : strVals) {
                T convertedVal = (T) getArgBundle().converters().get(_option.getDataType()).convert(str);            
                resultList.add(convertedVal);
            }
            return resultList;
        }

        return null;
    }
    
    public Object getArgByName(CharSequence _optionName) {
        return Optional.ofNullable(requireParsed(this).getOption(_optionName))
                .map(this::getArg)
                .orElseThrow(() -> optionNotDefined(_optionName));
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
        if (getArgBundle().knownArgs().containsKey(requireParsed.getArgBundle().options().get(requireOption(_option).getName()))) {
            return true;
        } else if (getArgBundle().knownMultiArgs().containsKey(requireParsed.getArgBundle().options().get(requireOption(_option).getName()))) {
            return true;
        }
        
        return Optional.ofNullable(requireParsed.getArgBundle().options().get(requireOption(_option).getShortName()))
                .map(k -> getArgBundle().knownArgs().containsKey(k) || getArgBundle().knownMultiArgs().containsKey(k))
                .orElseThrow(() -> optionNotDefined(_option));
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
        
        if (getArgBundle().knownArgs().containsKey(_option)) {
            return 1;
        }
        if (getArgBundle().knownMultiArgs().containsKey(_option)) {
            return getArgBundle().knownMultiArgs().get(_option).size();
        }

        return 0;
    }


    private ParsedArg parseArg(String _token, boolean _handle) {
        if (_token == null) {
            return new ParsedArg(false, false, null);
        }
        
        Matcher matcher = getLongOptPattern().matcher(_token);
        if (matcher.matches()) {
            return new ParsedArg(true, false, getArgBundle().options().get(matcher.group(1)));
        } else {
            matcher = getShortOptPattern().matcher(_token);
            
            if (matcher.matches()) {
                if (matcher.group(1).length() > 1) {
                    CmdArgOption<?> cmdArgOption = null;
                    CmdArgOption<?> prevOption = null;
                    // iterate all combined short options (e.g. -abcd)
                    for (char c : matcher.group(1).toCharArray()) {
                        String key = c + "";
                        cmdArgOption = getArgBundle().options().get(key);
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
                                getArgBundle().unknownArgs().put(key, null);
                            }
                        }
                    }
                    return new ParsedArg(true, true, cmdArgOption);
                } else {
                    CmdArgOption<?> cmdArgOption = getArgBundle().options().get(matcher.group(1));
                    if (cmdArgOption == null) { // unknown argument used
                        getArgBundle().unknownArgs().put(_token, null);
                        return new ParsedArg(false, false, null);
                    }

                    return new ParsedArg(true, false, cmdArgOption);
                }
            }
        }
        return new ParsedArg(false, false, null);
    }
   
    private CommandLine logResults() {
        if (getLogger().isDebugEnabled()) {
            Map<String, String> kargs = new LinkedHashMap<>();
            Map<String, String> margs = new LinkedHashMap<>();
            
            for (Entry<CmdArgOption<?>, String> e : getArgBundle().knownArgs().entrySet()) {
                kargs.put(formatOption(e.getKey(), getLongOptPrefix(), getShortOptPrefix()), e.getValue());
            }

            for (Entry<CmdArgOption<?>, List<String>> e : getArgBundle().knownMultiArgs().entrySet()) {
                margs.put(formatOption(e.getKey(), getLongOptPrefix(), getShortOptPrefix()), String.join(", ", e.getValue()));
            }

            getLogger().debug("knownArgs:      {}", kargs);
            getLogger().debug("knownMultiArgs: {}", margs);

            getLogger().debug("unknownArgs:    {}", getArgBundle().unknownArgs());
            getLogger().debug("unknownTokens:  {}", getArgBundle().unknownTokens());
            getLogger().debug("dupArgs:        {}", getArgBundle().dupArgs());
        }
        return this;
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
        if (isFailOnUnknownArg() && !getArgBundle().unknownArgs().isEmpty()) {
            failures.add("unknown arguments: " + getArgBundle().unknownArgs().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        }
        if (isFailOnUnknownToken() && !getArgBundle().unknownTokens().isEmpty()) {
            failures.add("unknown tokens: " + String.join(", ", getArgBundle().unknownTokens()));
        }
        if (isFailOnDupArg() && !getArgBundle().dupArgs().isEmpty()) {
            failures.add("duplicate arguments: " + getArgBundle().dupArgs().keySet().stream().map(x -> formatOption(x, getLongOptPrefix(), getShortOptPrefix())).collect(Collectors.joining(", ")));
        }

        // check all required options are given
        List<String> missingOptions = getOptions().values().stream().filter(CmdArgOption::isRequired)
                .filter(s -> !getArgBundle().knownArgs().containsKey(s))
                .map(CmdArgOption::getName)
                .collect(Collectors.toList());
        if (!missingOptions.isEmpty()) {
            failures.add("required options missing: " + String.join(", ", missingOptions));
        }

        // check values
        for (Entry<CmdArgOption<?>, String> knownArg : getArgBundle().knownArgs().entrySet()) {
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
                            + getArgBundle().knownArgs().get(knownArg.getKey()) + ")");
                }
            }
        }

        if (!failures.isEmpty()) {
            throw createException("Parsing of command-line failed: " + String.join(", ", failures), getExceptionType());
        }
        return this;
    }

   

}

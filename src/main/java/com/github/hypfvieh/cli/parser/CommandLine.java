package com.github.hypfvieh.cli.parser;

import java.io.PrintWriter;
import java.lang.invoke.MethodType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class CommandLine {

    private final Logger                             logger             = LoggerFactory.getLogger(getClass());

    private final Map<String, CmdArgOption<?>>       options            = new LinkedHashMap<>();

    private final Map<CmdArgOption<?>, String>       knownArgs          = new LinkedHashMap<>();
    private final Map<CmdArgOption<?>, List<String>> knownMultiArgs     = new LinkedHashMap<>();
    
    private final Map<String, String>                unknownArgs        = new LinkedHashMap<>();
    private final List<String>                       unknownTokens      = new ArrayList<>();
    private final Map<CmdArgOption<?>, String>       dupArgs            = new LinkedHashMap<>();
    private final List<CmdArgOption<?>>              missingArgs        = new ArrayList<>();

    private boolean                                  failOnUnknownArg   = true;
    private boolean                                  failOnUnknownToken = true;
    private boolean                                  failOnDupArg       = true;

    private String                                   longOptPrefix      = null;
    private String                                   shortOptPrefix     = null;
    private Pattern                                  longOptPattern     = null;
    private Pattern                                  shortOptPattern    = null;
    private Class<? extends RuntimeException>        exceptionType      = CommandLineException.class;

    private final Map<Class<?>, Function<String, ?>> converters         = new HashMap<>();

    private final AtomicBoolean                      parsed             = new AtomicBoolean(false);

    public CommandLine() {
        registerDefaultConverters();
        withLongOptPrefix("--");
        withShortOptPrefix("-");
    }

    public CommandLine addOption(CmdArgOption<?> _option) {
        requireOption(_option);
        if (_option.getName() != null) {
            options.put(_option.getName(), _option);    
        }
        if (_option.getShortName() != null) {
            options.put(_option.getShortName(), _option);
        }
        
        logger.debug("Added {} command-line option '{}': {}",
                _option.isRequired() ? "required" : "optional", _option.getName(), _option.getDescription());
        return this;
    }

    public CommandLine addOptions(CmdArgOption<?>... _options) {
        if (_options != null) {
            for (CmdArgOption<?> option : _options) {
                addOption(option);
            }
        }
        return this;
    }

    boolean hasOption(CharSequence _optionName) {
        return getOption(_optionName) != null;
    }

    public boolean hasOption(CmdArgOption<?> _option) {
        return getOption(requireOption(_option).getName()) != null;
    }

    public boolean isFailOnUnknownArg() {
        return failOnUnknownArg;
    }

    public CommandLine withFailOnUnknownArg(boolean _failOnUnknownArg) {
        failOnUnknownArg = _failOnUnknownArg;
        return this;
    }

    public boolean isFailOnUnknownToken() {
        return failOnUnknownToken;
    }

    public CommandLine withFailOnUnknownToken(boolean _failOnUnknownToken) {
        failOnUnknownToken = _failOnUnknownToken;
        return this;
    }

    public boolean isFailOnDupArg() {
        return failOnDupArg;
    }

    public CommandLine withFailOnDupArg(boolean _failOnDupArg) {
        failOnDupArg = _failOnDupArg;
        return this;
    }

    public CommandLine withShortOptPrefix(String _prefix) {
        shortOptPrefix = _prefix;
        shortOptPattern = Pattern.compile("^(?:" + Pattern.quote(_prefix) + "(.+))");
        return this;
    }

    public CommandLine withLongOptPrefix(String _prefix) {
        longOptPrefix = _prefix;
        longOptPattern = Pattern.compile("^(?:" + Pattern.quote(_prefix) + "(.+))");
        return this;
    }

    public Class<? extends RuntimeException> getExceptionType() {
        return exceptionType;
    }

    public CommandLine setExceptionType(Class<? extends RuntimeException> _exceptionType) {
        Objects.requireNonNull(_exceptionType, "Exception type required");
        try {
            _exceptionType.getConstructor(String.class);
        } catch (NoSuchMethodException | SecurityException _ex) {
            throw new CommandLineException("Exception type requires a single-argument constructor of type String");
        }
        exceptionType = _exceptionType;
        return this;
    }

    private void registerDefaultConverters() {
        registerConverter(boolean.class, t -> t != null && t.trim().toLowerCase().matches("^(?:true|yes|1)$"));
        registerConverter(byte.class, Byte::parseByte);
        registerConverter(short.class, Short::parseShort);
        registerConverter(int.class, Integer::parseInt);
        registerConverter(long.class, Long::parseLong);
        registerConverter(float.class, Float::parseFloat);
        registerConverter(double.class, Double::parseDouble);
        registerConverter(String.class, s -> s);
        registerConverter(LocalDate.class, s -> parseDateTime(s,
                (t, p) -> LocalDate.parse(t, DateTimeFormatter.ofPattern(p)), "yyyy-MM-dd", "yyyyMMdd"));
        registerConverter(LocalDateTime.class,
                s -> parseDateTime(s, (t, p) -> LocalDateTime.parse(t, DateTimeFormatter.ofPattern(p)),
                        "yyyy-MM-dd HH:mm:ss", "yyyyMMddHHmmss", "yyyyMMddHHmm"));
        registerConverter(LocalTime.class, s -> parseDateTime(s,
                (t, p) -> LocalTime.parse(t, DateTimeFormatter.ofPattern(p)), "HH:mm:ss", "HHmmss", "HHmm"));
    }

    private <R> R parseDateTime(String _input, BiFunction<CharSequence, String, R> _parseFunc, String... _patterns) {
        Throwable ex = null;
        if (_input.matches("^[0-9\\-]+$")) {
            for (String p : _patterns) {
                try {
                    return _parseFunc.apply(_input, p);
                } catch (RuntimeException _ex) {
                    ex = _ex;
                }
            }
        }
        StringBuilder sb = new StringBuilder("Failed to parse date/time argument '" + _input + "'");
        if (ex != null) {
            sb.append(" (" + ex + ")");
        }
        throw createException(sb.toString());
    }

    public <T> CommandLine registerConverter(Class<T> _type, Function<String, T> _function) {
        Objects.requireNonNull(_type, "Type required");
        Objects.requireNonNull(_function, "Function required");
        converters.put(_type, _function);
        if (_type.isPrimitive()) {
            return registerConverter(uncheckedCast((Class<?>) MethodType.methodType(_type).wrap().returnType()),
                    _function);
        }
        return this;
    }

    CommandLine parse(String _args) {
        return parse(_args == null ? null : _args.split(" "));
    }

    public synchronized CommandLine parse(String[] _args) {
        logger.debug("Parsing command-line: {}", Arrays.toString(_args));

        reset();

        if (_args != null) {
            final int argsLen = _args.length;
            
            for (int i = 0; i < argsLen; i++) {
                logger.trace("Token {}/{}: {}", i, argsLen, _args[i]);

                String token = Optional.ofNullable(_args[i]).map(String::trim).orElse("");
                
                if (token.isEmpty()) {
                    continue;
                }
                
                ParsedArg parsedArg = parseArg(token);
                CmdArgOption<?> cmdOpt = parsedArg.getCmdArgOpt();
                
                if (argsLen -1 >= i+1) {
                    String val = _args[i+1];
                    ParsedArg nextArg = parseArg(val);
                    
                    if (nextArg.getCmdArgOpt() == null) { // looks like proper value
                        if (cmdOpt != null) {
                            handleCmdOption(cmdOpt, val);
                            i++;
                        } else if (parsedArg.isLookingLikeOption()) {
                            unknownArgs.put(token, val);
                            i++;
                        } else {
                            unknownTokens.add(token);
                        }
                    } else { // next token is an option too
                        if (cmdOpt != null) {
                            if (cmdOpt.hasValue()) { // command needs option, but got another option
                                missingArgs.add(cmdOpt);
                            } else { // no arguments required for option
                                handleCmdOption(cmdOpt, null);
                            }
                        } else {
                            unknownTokens.add(token);
                        }
                    }
                } else { // no arguments left
                    if (cmdOpt != null && cmdOpt.hasValue()) { // command needs option, but got another option
                        missingArgs.add(cmdOpt);
                    } else if (!parsedArg.isLookingLikeOption()) {
                        unknownTokens.add(token);
                    } else if (!parsedArg.isMultiArg()) {
                        handleCmdOption(cmdOpt, null);
                    }
                }
            }
        }

        parsed.set(true);

        logResults();
        validate();

        return this;
    }

    private void handleCmdOption(CmdArgOption<?> _cmdOpt, String _val) {
        if (_cmdOpt.isRepeatable()) {
            knownMultiArgs.computeIfAbsent(_cmdOpt, x -> new ArrayList<>()).add(trimToNull(_val));
        } else if (!_cmdOpt.isRepeatable() && !knownArgs.containsKey(_cmdOpt)) {
            knownArgs.put(_cmdOpt, trimToNull(_val));
        } else {
            dupArgs.put(_cmdOpt, _val);
        }
    }

    private String trimToNull(String _val) {
        if (_val == null || _val.isBlank()) {
            return null;
        }
        return _val;
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
            List<String> list = knownMultiArgs.get(_option);
            if (list != null && !list.isEmpty()) {
                strVals.addAll(list);
            }
        } else {
            String val = knownArgs.get(_option);
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
                T convertedVal = (T) converters.get(_option.getDataType()).apply(str);            
                resultList.add(convertedVal);
            }
            return resultList;
        }

        return null;
    }
    
    Object getArg(CharSequence _optionName) {
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
        if (knownArgs.containsKey(requireParsed.options.get(requireOption(_option).getName()))) {
            return true;
        } else if (knownMultiArgs.containsKey(requireParsed.options.get(requireOption(_option).getName()))) {
            return true;
        }
        
        return Optional.ofNullable(requireParsed.options.get(requireOption(_option).getShortName()))
                .map(k -> knownArgs.containsKey(k) || knownMultiArgs.containsKey(k))
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
        
        if (knownArgs.containsKey(_option)) {
            return 1;
        }
        if (knownMultiArgs.containsKey(_option)) {
            return knownMultiArgs.get(_option).size();
        }

        return 0;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Class<T> uncheckedCast(Class<?> _type) {
        return (Class<T>) _type;
    }

    private ParsedArg parseArg(String token) {
        if (token == null) {
            return new ParsedArg(false, false, null);
        }
        
        Matcher matcher = longOptPattern.matcher(token);
        if (matcher.matches()) {
            return new ParsedArg(true, false, options.get(matcher.group(1)));
        } else {
            matcher = shortOptPattern.matcher(token);
            
            if (matcher.matches()) {
                if (token.length() > 1) {
                    String sb = null;
                    CmdArgOption<?> cmdArgOption = null;
                    for (char c : token.toCharArray()) {
                        String key = c + "";
                        cmdArgOption = options.get(key);
                        if (cmdArgOption != null) {
                            if (!cmdArgOption.hasValue()) {
                                handleCmdOption(cmdArgOption, null);
                            } else if (sb == null && cmdArgOption.hasValue()) {
                                sb = key;
                            }
                        } 
                    }
                    return new ParsedArg(true, true, cmdArgOption);
                } else {
                    CmdArgOption<?> cmdArgOption = options.get(token);
                    if (cmdArgOption == null) { // unknown argument used
                        unknownArgs.put(token, null);
                        return null;
                    }

                    return new ParsedArg(true, false, cmdArgOption);
                }
            }
        }
        return new ParsedArg(false, false, null);
    }
    
    private CommandLine reset() {
        return accessSync(t -> {
            unknownTokens.clear();
            Stream.of(knownArgs, unknownArgs, dupArgs).forEach(Map::clear);
            return t;
        });
    }

    private CommandLine logResults() {
        if (logger.isDebugEnabled()) {
            Map<String, String> kargs = new LinkedHashMap<>();
            Map<String, String> margs = new LinkedHashMap<>();
            
            for (Entry<CmdArgOption<?>, String> e : knownArgs.entrySet()) {
                kargs.put(printableArgName(e.getKey()), e.getValue());
            }

            for (Entry<CmdArgOption<?>, List<String>> e : knownMultiArgs.entrySet()) {
                margs.put(printableArgName(e.getKey()), String.join(", ", e.getValue()));
            }

            logger.debug("knownArgs:      {}", kargs);
            logger.debug("knownMultiArgs: {}", margs);

            logger.debug("unknownArgs:    {}", unknownArgs);
            logger.debug("unknownTokens:  {}", unknownTokens);
            logger.debug("dupArgs:        {}", dupArgs);
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
        if (failOnUnknownArg && !unknownArgs.isEmpty()) {
            failures.add("unknown arguments: " + unknownArgs.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        }
        if (failOnUnknownToken && !unknownTokens.isEmpty()) {
            failures.add("unknown tokens: " + String.join(", ", unknownTokens));
        }
        if (failOnDupArg && !dupArgs.isEmpty()) {
            failures.add("duplicate arguments: " + dupArgs.keySet().stream().map(this::formatOption).collect(Collectors.joining(", ")));
        }

        // check all required options are given
        List<String> missingOptions = streamOptions(CmdArgOption::isRequired)
                .filter(s -> !knownArgs.containsKey(s))
                .map(CmdArgOption::getName)
                .collect(Collectors.toList());
        if (!missingOptions.isEmpty()) {
            failures.add("required options missing: " + String.join(", ", missingOptions));
        }

        // check values
        for (Entry<CmdArgOption<?>, String> knownArg : knownArgs.entrySet()) {
            CmdArgOption<?> option = knownArg.getKey();
            if (option.hasValue() && knownArg.getValue() == null && option.getDefaultValue() == null) {
                failures.add("argument '" + formatOption(knownArg.getKey()) + "' requires a value");
            } else if (!option.hasValue() && knownArg.getValue() != null) {
                failures.add("argument '" + formatOption(knownArg.getKey()) + "' cannot have a value");
            }
            // check value type
            if (option.hasValue() && knownArg.getValue() != null) {
                try {
                    getArg(option);
                } catch (Exception _ex) {
                    failures.add("argument '" + formatOption(knownArg.getKey()) + "' has invalid value ("
                            + knownArgs.get(knownArg.getKey()) + ")");
                }
            }
        }

        if (!failures.isEmpty()) {
            throw createException("Parsing of command-line failed: " + String.join(", ", failures));
        }
        return this;
    }

    private String formatOption(CmdArgOption<?> _arg) {
        if (_arg == null) {
            return null;
        } else if (_arg.getName() != null && !_arg.getName().isBlank() && _arg.getShortName() != null && !_arg.getShortName().isBlank()) {
            return longOptPrefix + _arg.getName() + "/" + shortOptPrefix + _arg.getShortName();
        } else if (_arg.getName() != null && !_arg.getName().isBlank()) {
            return longOptPrefix + _arg.getName();
        } else if (_arg.getShortName() != null && !_arg.getShortName().isBlank()) {
            return shortOptPrefix + _arg.getShortName();   
        } else {
            return "?";
        }
    }
    
    private static <T> T requireOption(T _option) {
        T o = Objects.requireNonNull(_option, "Option required");
        if (o instanceof CmdArgOption<?> opt) {
            if ((opt.getName() == null || opt.getName().isBlank()) && (opt.getShortName() == null || opt.getShortName().isBlank())) {
                throw new IllegalArgumentException("Command-line option requires a name or shortname: " + _option);
            }
        }
        return o;
    }

    private static CommandLine requireParsed(CommandLine _c) {
        if (!_c.parsed.get()) {
            throw _c.createException("Command-line not parsed");
        }
        return _c;
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
    
    private static CommandLineException optionNotDefined(Object _option) {
        return new CommandLineException("Option not defined: " + _option);
    }

    private RuntimeException createException(String _message) {
        if (CommandLineException.class.isInstance(exceptionType)) {
            return new CommandLineException(_message);
        }
        try {
            return exceptionType.getConstructor(String.class).newInstance(_message);
        } catch (Exception _ex) {
            return new CommandLineException(_message);
        }
    }

    private <R> R accessSync(Function<CommandLine, R> _function) {
        Objects.requireNonNull(_function, "Function required");
        synchronized (this) {
            return _function.apply(this);
        }
    }

    public Map<String, CmdArgOption<?>> getOptions() {
        return accessSync(t -> Collections.unmodifiableMap(t.options));
    }

    public CmdArgOption<?> getOption(CharSequence _optionName) {
        return accessSync(t -> options.get(Objects.requireNonNull(_optionName, "Option name required")));
    }

    Stream<CmdArgOption<?>> streamOptions(Predicate<? super CmdArgOption<?>> _predicate) {
        return getOptions().values().stream()
                .filter(o -> _predicate == null || _predicate.test(o));
    }

    public Map<CmdArgOption<?>, String> getKnownArgs() {
        return accessSync(t -> Collections.unmodifiableMap(requireParsed(t).knownArgs));
    }

    public Map<String, String> getUnknownArgs() {
        return accessSync(t -> requireParsed(t).unknownArgs);
    }

    public List<String> getUnknownTokens() {
        return accessSync(t -> requireParsed(t).unknownTokens);
    }

    public Map<CmdArgOption<?>, String> getDupArgs() {
        return accessSync(t -> requireParsed(t).dupArgs);
    }

    public void printHelp(String _mainClassName) {
        try (PrintWriter pw = new PrintWriter(System.out)) {
            pw.print(getHelp(_mainClassName));
            pw.flush();
        }
    }

    public String getHelp(String _mainClassName) {
        List<String> required = streamOptions(CmdArgOption::isRequired)
                .map(o -> "--" + o.getName() + (o.hasValue() ? " <arg>" : ""))
                .collect(Collectors.toList());
        List<String> optional = streamOptions(CmdArgOption::isOptional)
                .map(o -> "--" + o.getName() + (o.hasValue() ? " <arg>" : ""))
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder()
                .append("usage: " + Optional.ofNullable(_mainClassName).orElseGet(CommandLine::getMainClassName));
        if (!required.isEmpty()) {
            sb.append(" " + String.join(" ", required));
        }
        if (!optional.isEmpty()) {
            sb.append(" [" + String.join(" ", optional) + "]");
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Returns the simple class name of the topmost stack element.
     * 
     * @return simple class name
     */
    private static String getMainClassName() {
        StackTraceElement[] stackTrace = new Throwable().fillInStackTrace().getStackTrace();
        String mainClassName = stackTrace[stackTrace.length - 1].getClassName();
        int idx = mainClassName.lastIndexOf(".");
        if (idx > -1) {
            mainClassName = mainClassName.substring(idx + 1);
        }
        return mainClassName;
    }

}

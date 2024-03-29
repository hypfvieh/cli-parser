package com.github.hypfvieh.cli.parser;

import static com.github.hypfvieh.cli.parser.StaticUtils.*;

import com.github.hypfvieh.cli.parser.converter.*;
import com.github.hypfvieh.cli.parser.formatter.DefaultHelpFormatter;
import com.github.hypfvieh.cli.parser.formatter.DefaultUsageFormatter;
import com.github.hypfvieh.cli.parser.formatter.IUsageFormatter;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Base class of every command line.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 *
 * @param <B> concrete command line implementation
 */
public abstract class AbstractBaseCommandLine<B extends AbstractBaseCommandLine<?>> {

    private final Logger                      logger             = System.getLogger(getClass().getName());

    private final ArgumentBundle              argBundle          = new ArgumentBundle();
    private final AtomicBoolean               parsed             = new AtomicBoolean(false);

    private boolean                           failOnUnknownArg   = true;
    private boolean                           failOnUnknownToken = true;
    private boolean                           failOnDupArg       = true;

    private String                            longOptPrefix      = null;
    private String                            shortOptPrefix     = null;
    private Pattern                           longOptPattern     = null;
    private Pattern                           shortOptPattern    = null;
    private Class<? extends RuntimeException> exceptionType      = CommandLineException.class;

    private IUsageFormatter                   usageFormatter     = new DefaultUsageFormatter();
    private IUsageFormatter                   helpFormatter      = new DefaultHelpFormatter();

    /**
     * Default constructor with long prefix {@code --} and short prefix {@code -}.
     */
    public AbstractBaseCommandLine() {
        registerDefaultConverters();
        withLongOptPrefix("--");
        withShortOptPrefix("-");
    }

    /**
     * A reference to ourselves to allow chaining with subclasses.
     *
     * @return this
     */
    protected abstract B self();

    /**
     * Registers some default converters.
     */
    private void registerDefaultConverters() {
        registerConverter(boolean.class, t -> t != null && t.trim().toLowerCase().matches("^(?:true|yes|1)$"));
        registerConverter(byte.class, Byte::parseByte);
        registerConverter(short.class, Short::parseShort);
        registerConverter(int.class, Integer::parseInt);
        registerConverter(long.class, Long::parseLong);
        registerConverter(float.class, Float::parseFloat);
        registerConverter(double.class, new DoubleConverter());
        registerConverter(String.class, s -> s);
        registerConverter(LocalDate.class, new LocalDateConverter());
        registerConverter(LocalDateTime.class, new LocalDateTimeConverter());
        registerConverter(LocalTime.class, new LocalTimeConverter());
        registerConverter(Pattern.class, Pattern::compile);
        registerConverter(Class.class, new ClassNameToInstanceConverter<>());
    }

    /**
     * Reset the internal state.
     *
     * @return this
     */
    B reset() {
        return accessSync(t -> {
            getArgBundle().getUnknownTokens().clear();
            Stream.of(getArgBundle().getKnownArgs(),
                getArgBundle().getUnknownArgs(),
                getArgBundle().getDupArgs(),
                getArgBundle().getKnownMultiArgs())
                .forEach(Map::clear);
            return t;
        });
    }

    /**
     * Registers a converter to convert an option argument String to a specific java object type.
     *
     * @param <T> type
     * @param _type java class to convert to
     * @param _converter converter instance
     *
     * @return this
     */
    public <T> B registerConverter(Class<T> _type, IValueConverter<T> _converter) {
        Objects.requireNonNull(_type, "Type required");
        Objects.requireNonNull(_converter, "Converter required");
        argBundle.getConverters().put(_type, _converter);
        if (_type.isPrimitive()) {
            return registerConverter(uncheckedCast((Class<?>) MethodType.methodType(_type).wrap().returnType()),
                _converter);
        }
        return self();
    }

    /**
     * Set the parsed state.
     *
     * @param _b true to signal that commandline was parsed
     */
    protected void setParsed(boolean _b) {
        parsed.set(_b);
    }

    /**
     * Add an option to the supported options.
     *
     * @param _option option, never null
     *
     * @return this
     *
     * @throws RuntimeException when option is not unique (short/long name was used by another option)
     */
    public B addOption(CmdArgOption<?> _option) {
        requireOption(_option);
        requireUniqueOption(_option, this);

        Optional.ofNullable(_option.getName())
            .ifPresent(n -> getArgBundle().getOptions().put(n, _option));
        Optional.ofNullable(_option.getShortName())
            .ifPresent(n -> getArgBundle().getOptions().put(n, _option));

        if (_option.getDataType() != null && _option.getDataType().isEnum() && !getArgBundle().getConverters().containsKey(_option.getDataType())) {
            @SuppressWarnings("unchecked")
            Class<Enum<?>> enumType = (Class<Enum<?>>) _option.getDataType();
            getArgBundle().getConverters().put(_option.getDataType(), new EnumConverter(enumType));
        }

        getLogger().log(Level.DEBUG, "Added {0} command-line option ''{1}'': {2}",
            _option.isRequired() ? "required" : "optional", _option.getName(), _option.getDescription());
        return self();
    }

    /**
     * Adds multiple options to the supported options.
     *
     * @param _options options to add
     *
     * @return this
     *
     * @throws RuntimeException when option is not unique (short/long name was used by another option)
     */
    public B addOptions(CmdArgOption<?>... _options) {
        if (_options != null) {
            for (CmdArgOption<?> option : _options) {
                addOption(option);
            }
        }
        return self();
    }

    /**
     * Prints some debug statements to the configured logger.
     *
     * @return this
     */
    protected B logResults() {
        if (getLogger().isLoggable(Level.DEBUG)) {
            Map<String, String> kargs = new LinkedHashMap<>();
            Map<String, String> margs = new LinkedHashMap<>();

            for (Entry<CmdArgOption<?>, String> e : getArgBundle().getKnownArgs().entrySet()) {
                kargs.put(formatOption(e.getKey(), getLongOptPrefix(), getShortOptPrefix()), e.getValue());
            }

            for (Entry<CmdArgOption<?>, List<String>> e : getArgBundle().getKnownMultiArgs().entrySet()) {
                margs.put(formatOption(e.getKey(), getLongOptPrefix(), getShortOptPrefix()), String.join(", ", e.getValue()));
            }

            getLogger().log(Level.DEBUG, "knownArgs:      {0}", kargs);
            getLogger().log(Level.DEBUG, "knownMultiArgs: {0}", margs);

            getLogger().log(Level.DEBUG, "unknownArgs:    {0}", getArgBundle().getUnknownArgs());
            getLogger().log(Level.DEBUG, "unknownTokens:  {0}", getArgBundle().getUnknownTokens());
            getLogger().log(Level.DEBUG, "dupArgs:        {0}", getArgBundle().getDupArgs());
        }
        return self();
    }

    /**
     * Checks if there is any option with the given name.
     *
     * @param _optionName option name to check
     * @return true if present, false otherwise
     */
    public boolean hasOption(CharSequence _optionName) {
        return getOption(_optionName) != null;
    }

    /**
     * Checks if the given option is already present.
     *
     * @param _option option to check
     * @return true if present, false otherwise
     */
    public boolean hasOption(CmdArgOption<?> _option) {
        return getOption(requireOption(_option).getName()) != null;
    }

    /**
     * Returns a unmodifiable Map of all configured options.
     *
     * @return unmodifiable Map, never null
     */
    public Map<String, CmdArgOption<?>> getOptions() {
        return accessSync(t -> Collections.unmodifiableMap(t.getArgBundle().getOptions()));
    }

    /**
     * Returns a option using its name.
     *
     * @param _optionName option name
     * @return option, maybe null
     */
    public CmdArgOption<?> getOption(CharSequence _optionName) {
        return accessSync(t -> getArgBundle().getOptions().get(Objects.requireNonNull(_optionName, "Option name required")));
    }

    /**
     * Returns a unmodifiable Map of all successfully parsed, known arguments.
     *
     * @return unmodifiable Map, never null
     */
    public Map<CmdArgOption<?>, String> getKnownArgs() {
        return accessSync(t -> Collections.unmodifiableMap(requireParsed(t).getArgBundle().getKnownArgs()));
    }

    /**
     * Returns a unmodifiable Map of all parsed, but unknown arguments.<br>
     * The value of the map will represent the parsed value, or null if no value found.
     *
     * @return unmodifiable Map, never null
     */
    public Map<String, String> getUnknownArgs() {
        return accessSync(t -> Collections.unmodifiableMap(requireParsed(t).getArgBundle().getUnknownArgs()));
    }

    /**
     * Returns a unmodifiable list of all unknown option arguments.
     *
     * @return unmodifiable list, never null
     */
    public List<String> getUnknownTokens() {
        return accessSync(t -> Collections.unmodifiableList(requireParsed(t).getArgBundle().getUnknownTokens()));
    }

    /**
     * Returns a unmodifiable Map of all parsed, known but duplicated arguments. <br>
     * The value of the map will represent the parsed value, or null if no value found.<br>
     * Only arguments which are not repeatable will be added to the duplicate list.
     *
     * @return unmodifiable Map, never null
     */
    public Map<CmdArgOption<?>, String> getDupArgs() {
        return accessSync(t -> requireParsed(t).getArgBundle().getDupArgs());
    }

    /**
     * Prints the "usage" of the program to stdout. Will try to find the main class using the current stack.
     */
    public void printUsage() {
        printUsage(null, System.out);
    }

    /**
     * Prints the "usage" of the program to the given output.
     *
     * @param _mainClassName name of program or main class
     * @param _output output stream to write to
     */
    public void printUsage(String _mainClassName, OutputStream _output) {
        Objects.requireNonNull(_output, "Output required");
        try (PrintWriter pw = new PrintWriter(_output, false, StandardCharsets.UTF_8)) {
            pw.print(getUsage(_mainClassName));
            pw.flush();
        }
    }

    /**
     * Creates the "usage" String using the configured formatter.
     *
     * @param _mainClassName name of program or main class
     * @return usage String
     */
    public String getUsage(String _mainClassName) {
        return usageFormatter.format(new ArrayList<>(getOptions().values()),
            getLongOptPrefix(), getShortOptPrefix(),
            Optional.ofNullable(_mainClassName).orElseGet(IUsageFormatter::getMainClassName));
    }

    /**
     * Creates a multiline String containing all options and descriptions configured.<br>
     * <p>
     * Indented to be used in "help" like commands. <br>
     * Uses the configured {@link #helpFormatter}.
     * </p>
     *
     * @param _mainClassName name of program or main class
     * @return usage String
     */
    public String getArgumentHelp(String _mainClassName) {
        return helpFormatter.format(new ArrayList<>(getOptions().values()),
            getLongOptPrefix(), getShortOptPrefix(),
            Optional.ofNullable(_mainClassName).orElseGet(IUsageFormatter::getMainClassName));
    }

    /**
     * Setup a different usage formatter.
     *
     * @param _formatter formatter, null will be ignored
     *
     * @return this
     */
    public B withUsageFormatter(IUsageFormatter _formatter) {
        if (_formatter != null) {
            usageFormatter = Objects.requireNonNull(_formatter, "Formatter required");
        }
        return self();
    }

    /**
     * Setup a different help formatter.
     *
     * @param _formatter formatter, null will be ignored
     *
     * @return this
     */
    public B withHelpFormatter(IUsageFormatter _formatter) {
        if (_formatter != null) {
            helpFormatter = Objects.requireNonNull(_formatter, "Formatter required");
        }
        return self();
    }

    /**
     * Specifies if command line parsing should fail when an unknown argument was found.
     * <p>
     * Default: true
     * </p>
     *
     * @param _failOnUnknownArg true to enable
     *
     * @return this
     */
    public B withFailOnUnknownArg(boolean _failOnUnknownArg) {
        failOnUnknownArg = _failOnUnknownArg;
        return self();
    }

    /**
     * Specifies if command line parsing should fail when an unknown token was found.
     * <p>
     * Default: true
     * </p>
     *
     * @param _failOnUnknownToken true to enable
     *
     * @return this
     */
    public B withFailOnUnknownToken(boolean _failOnUnknownToken) {
        failOnUnknownToken = _failOnUnknownToken;
        return self();
    }

    /**
     * Specifies if command line parsing should fail when an duplicate argument was found.
     * <p>
     * Default: true
     * </p>
     *
     * @param _failOnDupArg true to enable
     *
     * @return this
     */
    public B withFailOnDupArg(boolean _failOnDupArg) {
        failOnDupArg = _failOnDupArg;
        return self();
    }

    /**
     * Defines the prefix for short-option names.
     * <p>
     * Default: -
     * </p>
     *
     * @param _prefix prefix for short options
     *
     * @return this
     */
    public B withShortOptPrefix(String _prefix) {
        if (_prefix == null || _prefix.isBlank()) {
            return self();
        }
        shortOptPrefix = _prefix;
        String qPrx = Pattern.quote(_prefix);
        // "^" + qPrx + "(?:(?!" + qPrx + "))(.+)"

        shortOptPattern = Pattern.compile("^" + qPrx + "(?:(?!" + qPrx + "))(?:([^=]*)=(.+)|(.+))");
        return self();
    }

    /**
     * Defines the prefix for long-option names.
     * <p>
     * Default: --
     * </p>
     *
     * @param _prefix prefix for long options
     *
     * @return this
     */
    public B withLongOptPrefix(String _prefix) {
        if (_prefix == null || _prefix.isBlank()) {
            return self();
        }
        longOptPrefix = _prefix;
        String qPrx = Pattern.quote(_prefix);
        // ^--(?:(?!-))(?:([^=]*)=(.+)|(.+))
        // "^" + qPrx + "(?:(?!" + Pattern.quote(_prefix.charAt(0) + "") + "))(.+)"
        longOptPattern = Pattern.compile("^" + qPrx + "(?:(?!" + Pattern.quote(_prefix.charAt(0) + "") + "))(?:([^=]*)=(.+)|(.+))");
        return self();
    }

    /**
     * Set a RuntimeException based exception class thrown when command line parsing fails.
     * <p>
     * Default: {@link CommandLineException}
     * </p>
     *
     * @param _exceptionType class, never null
     *
     * @return this
     */
    public B withExceptionType(Class<? extends RuntimeException> _exceptionType) {
        Objects.requireNonNull(_exceptionType, "Exception type required");
        try {
            _exceptionType.getConstructor(String.class);
        } catch (NoSuchMethodException | SecurityException _ex) {
            throw new CommandLineException("Exception type requires a single-argument constructor of type String");
        }
        exceptionType = _exceptionType;
        return self();
    }

    /**
     * Returns the current short option name prefix.
     *
     * @return String
     */
    public String getShortOptPrefix() {
        return shortOptPrefix;
    }

    /**
     * Returns the current long option name prefix.
     *
     * @return String
     */
    public String getLongOptPrefix() {
        return longOptPrefix;
    }

    /**
     * Returns the current pattern to parse long option names.
     *
     * @return Pattern
     */
    protected Pattern getLongOptPattern() {
        return longOptPattern;
    }

    /**
     * Returns the current pattern to parse short option names.
     *
     * @return Pattern
     */
    protected Pattern getShortOptPattern() {
        return shortOptPattern;
    }

    /**
     * Returns the class of the current configured exception.
     *
     * @return Class, never null
     */
    public Class<? extends RuntimeException> getExceptionType() {
        return exceptionType;
    }

    /**
     * Returns the logger instance.
     *
     * @return Logger
     */
    protected Logger getLogger() {
        return logger;
    }

    /**
     * Returns the argument bundle for collecting/managing parsed arguments.
     *
     * @return ArgumentBundle, never null
     */
    ArgumentBundle getArgBundle() {
        return argBundle;
    }

    /**
     * Signals if the command line has been parsed.
     *
     * @return true if it was parsed, false otherwise
     */
    protected boolean isParsed() {
        return parsed.get();
    }

    /**
     * Returns true when command line parsing fails on duplicated arguments.
     *
     * @return boolean
     */
    public boolean isFailOnDupArg() {
        return failOnDupArg;
    }

    /**
     * Returns true when command line parsing fails on unknown arguments.
     *
     * @return boolean
     */
    public boolean isFailOnUnknownArg() {
        return failOnUnknownArg;
    }

    /**
     * Returns true when command line parsing fails on unknown tokens.
     *
     * @return boolean
     */
    public boolean isFailOnUnknownToken() {
        return failOnUnknownToken;
    }

    /**
     * Execute a function on <code>this</code> object synchroniously.
     *
     * @param <R> type to return
     * @param _function function to execute
     *
     * @return object of type R, maybe null
     */
    private <R> R accessSync(Function<B, R> _function) {
        Objects.requireNonNull(_function, "Function required");
        synchronized (self()) {
            return _function.apply(self());
        }
    }

    /**
     * Tries to find a {@link CmdArgOption} using the given function and checks if the option was used in the parsed
     * command line.
     *
     * @param _argFunction argument supplier (never null)
     *
     * @return true if command was found and it was found in parsed command line, false otherwise
     */
    protected boolean hasArg(Function<B, CmdArgOption<?>> _argFunction) {
        Objects.requireNonNull(_argFunction, "Function required");
        CmdArgOption<?> cmdOption = _argFunction.apply(requireParsed(self()));
        return cmdOption != null && (getArgBundle().getKnownArgs().containsKey(cmdOption) || getArgBundle().getKnownMultiArgs().containsKey(cmdOption));
    }
}

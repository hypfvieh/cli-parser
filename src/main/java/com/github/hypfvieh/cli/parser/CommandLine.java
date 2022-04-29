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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to parse the Java command-line and access arguments by name and type using {@link Option}s.
 * <p>
 *
 * Example of working with a command-line with two options and a custom exception type:
 * <pre>
 *     CommandLine commandLine = new CommandLine()
 *          .setExceptionType(MyApplicationException.class)
 *          .addOption(Option.builder(String.class)
 *                  .name("req1")
 *                  .required(true)
 *                  .defaultValue("default")
 *                  .build())
 *          .addOption(Option.builder()
 *                  .name("opt1")
 *                  .required(false)
 *                  .build())
 *          .parse();
 * </pre>
 *
 * @since 1.0.0 - 2022-04-29
 */
public class CommandLine {

    private final Logger                             logger             = LoggerFactory.getLogger(getClass());

    private final Map<String, Option<?>>             options            = new LinkedHashMap<>();

    private final Map<String, String>                knownArgs          = new LinkedHashMap<>();
    private final Map<String, String>                unknownArgs        = new LinkedHashMap<>();
    private final List<String>                       unknownTokens      = new ArrayList<>();
    private final Map<String, String>                dupArgs            = new LinkedHashMap<>();

    private boolean                                  failOnUnknownArg   = true;
    private boolean                                  failOnUnknownToken = true;
    private boolean                                  failOnDupArg       = true;

    private Class<? extends RuntimeException>        exceptionType      = CommandLineException.class;

    private final Map<Class<?>, Function<String, ?>> converters         = new HashMap<>();

    private final AtomicBoolean                      parsed             = new AtomicBoolean(false);

    public CommandLine() {
        registerDefaultConverters();
    }

    public CommandLine addOption(Option<?> _option) {
        requireOption(_option);
        options.put(_option.getName(), _option);
        logger.debug("Added {} command-line option '{}': {}",
                _option.isRequired() ? "required" : "optional", _option.getName(), _option.getDescription());
        return this;
    }

    public CommandLine addOptions(Option<?>... _options) {
        if (_options != null) {
            for (Option<?> option : _options) {
                addOption(option);
            }
        }
        return this;
    }

    boolean hasOption(CharSequence _optionName) {
        return getOption(_optionName) != null;
    }

    public boolean hasOption(Option<?> _option) {
        return getOption(requireOption(_option).getName()) != null;
    }

    public boolean isFailOnUnknownArg() {
        return failOnUnknownArg;
    }

    public CommandLine setFailOnUnknownArg(boolean _failOnUnknownArg) {
        failOnUnknownArg = _failOnUnknownArg;
        return this;
    }

    public boolean isFailOnUnknownToken() {
        return failOnUnknownToken;
    }

    public CommandLine setFailOnUnknownToken(boolean _failOnUnknownToken) {
        failOnUnknownToken = _failOnUnknownToken;
        return this;
    }

    public boolean isFailOnDupArg() {
        return failOnDupArg;
    }

    public CommandLine setFailOnDupArg(boolean _failOnDupArg) {
        failOnDupArg = _failOnDupArg;
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
        registerConverter(LocalDate.class, s -> parseDateTime(s, (t, p) ->
                LocalDate.parse(t, DateTimeFormatter.ofPattern(p)), "yyyy-MM-dd", "yyyyMMdd"));
        registerConverter(LocalDateTime.class, s -> parseDateTime(s, (t, p) ->
                LocalDateTime.parse(t, DateTimeFormatter.ofPattern(p)), "yyyy-MM-dd HH:mm:ss", "yyyyMMddHHmmss", "yyyyMMddHHmm"));
        registerConverter(LocalTime.class, s -> parseDateTime(s, (t, p) ->
                LocalTime.parse(t, DateTimeFormatter.ofPattern(p)), "HH:mm:ss", "HHmmss", "HHmm"));
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
            return registerConverter(uncheckedCast((Class<?>) MethodType.methodType(_type).wrap().returnType()), _function);
        }
        return this;
    }

    CommandLine parse(String _args) {
        return parse(_args == null ? null : _args.split(" "));
    }

    public synchronized CommandLine parse(String[] _args) {
        logger.debug("Parsing command-line: {}", Arrays.toString(_args));

        reset();

        String lastArg = null;

        if (_args != null) {
            final int argsLen = _args.length;
            for (int i = 0; i < argsLen; i++) {
                logger.trace("Token {}/{}: {}", i, argsLen, _args[i]);
                String token = Optional.ofNullable(_args[i]).map(String::trim).orElse("");

                if (token.isEmpty()) {
                    continue;
                } else if (token.matches("^--.+")) { // argument starting with --
                    lastArg = handleOption(token.substring(2));
                //} else if (token.matches("^-.+")) { // argument starting with -
                //    lastArg = handleOption(token.substring(1));
                } else if (lastArg == null) { // orphan token (without prior argument)
                    unknownTokens.add(token);
                } else {
                    // handle option value
                    if (putArgValue(knownArgs, lastArg, token)) { // known argument without value
                        lastArg = null;
                    } else if (putArgValue(unknownArgs, lastArg, token)) { // unknown argument without value
                        lastArg = null;
                    } else if (putArgValue(dupArgs, lastArg, token)) { // duplicate argument without value
                        lastArg = null;
                    }
                }
            }
        }

        parsed.set(true);

        logResults();
        validate();

        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getArg(Option<T> _option) {
        Option<T> option = Optional.ofNullable((Option<T>) requireParsed(this).options.get(requireOption(_option).getName()))
                .orElseThrow(() -> optionNotDefined(_option));

        String strVal = knownArgs.get(option.getName());
        if (strVal == null) {
            return (T) option.getDefaultValue();
        }
        T convertedVal = (T) converters.get(option.getDataType()).apply(strVal);
        return option.getDataType().isPrimitive() ? convertedVal : (T) option.getDataType().cast(convertedVal);
    }

    Object getArg(CharSequence _optionName) {
        return Optional.ofNullable(requireParsed(this).getOption(_optionName))
                .map(this::getArg)
                .orElseThrow(() -> optionNotDefined(_optionName));
    }

    public boolean hasArg(Option<?> _option) {
        return Optional.ofNullable(requireParsed(this).options.get(requireOption(_option).getName()))
                .map(Option::getName)
                .map(knownArgs::containsKey)
                .orElseThrow(() -> optionNotDefined(_option));
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> uncheckedCast(Class<?> _type) {
        return (Class<T>) _type;
    }

    private CommandLine reset() {
        return accessSync(t -> {
            unknownTokens.clear();
            Stream.of(knownArgs, unknownArgs, dupArgs).forEach(Map::clear);
            return t;
        });
    }

    private static boolean putArgValue(Map<String, String> _argStore, String _arg, String _value) {
        if (_argStore.containsKey(_arg) && _argStore.get(_arg) == null) {
            _argStore.put(_arg, _value);
            return true;
        }
        return false;
    }

    private CommandLine logResults() {
        if (logger.isDebugEnabled()) {
            logger.debug("knownArgs:     {}", knownArgs);

            logger.debug("unknownArgs:   {}", unknownArgs);
            logger.debug("unknownTokens: {}", unknownTokens);
            logger.debug("dupArgs:       {}", dupArgs);
        }
        return this;
    }

    private CommandLine validate() {
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
            failures.add("duplicate arguments: " + String.join(", ", dupArgs.keySet()));
        }

        // check all required options are given
        List<String> missingOptions = streamOptions(Option::isRequired)
                .map(Option::getName)
                .filter(s -> !knownArgs.containsKey(s))
                .collect(Collectors.toList());
        if (!missingOptions.isEmpty()) {
            failures.add("required options missing: " + String.join(", ", missingOptions));
        }

        // check values
        for (Entry<String, String> knownArg : knownArgs.entrySet()) {
            Option<?> option = options.get(knownArg.getKey());
            if (option.hasValue() && knownArg.getValue() == null && option.getDefaultValue() == null) {
                failures.add("argument '" + knownArg.getKey() + "' requires a value");
            } else if (!option.hasValue() && knownArg.getValue() != null) {
                failures.add("argument '" + knownArg.getKey() + "' cannot have a value");
            }
            // check value type
            if (option.hasValue() && knownArg.getValue() != null) {
                try {
                    getArg(option);
                } catch (Exception _ex) {
                    failures.add("argument '" + knownArg.getKey() + "' has invalid value ("
                            + knownArgs.get(knownArg.getKey()) + ")");
                }
            }
        }

        if (!failures.isEmpty()) {
            throw createException("Parsing of command-line failed: " + String.join(", ", failures));
        }
        return this;
    }

    private static <T> T requireOption(T _option) {
        T o = Objects.requireNonNull(_option, "Option required");
        if (o instanceof Option<?> opt) {
            if (opt.getName() == null || opt.getName().isBlank()) {
                throw new IllegalArgumentException("Command-line option requires a name: " + _option);
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

    private String handleOption(String _arg) {
        if (knownArgs.containsKey(_arg)) {
            dupArgs.putIfAbsent(_arg, null);
            return _arg;
        } else if (!options.containsKey(_arg)) {
            unknownArgs.putIfAbsent(_arg, null);
            return _arg;
        } else {
            knownArgs.put(_arg, null);
            return _arg;
        }
    }

    public Map<String, Option<?>> getOptions() {
        return accessSync(t -> Collections.unmodifiableMap(t.options));
    }

    public Option<?> getOption(CharSequence _optionName) {
        return accessSync(t -> options.get(Objects.requireNonNull(_optionName, "Option name required")));
    }

    Stream<Option<?>> streamOptions(Predicate<? super Option<?>> _predicate) {
        return getOptions().values().stream()
                .filter(o -> _predicate == null || _predicate.test(o));
    }

    public Map<String, String> getKnownArgs() {
        return accessSync(t -> Collections.unmodifiableMap(requireParsed(t).knownArgs));
    }

    public Map<String, String> getUnknownArgs() {
        return accessSync(t -> requireParsed(t).unknownArgs);
    }

    public List<String> getUnknownTokens() {
        return accessSync(t -> requireParsed(t).unknownTokens);
    }

    public Map<String, String> getDupArgs() {
        return accessSync(t -> requireParsed(t).dupArgs);
    }

    public void printHelp(String _mainClassName) {
        try (PrintWriter pw = new PrintWriter(System.out)) {
            pw.print(getHelp(_mainClassName));
            pw.flush();
        }
    }

    public String getHelp(String _mainClassName) {
        List<String> required = streamOptions(Option::isRequired)
                .map(o -> "--" + o.getName() + (o.hasValue() ? " <arg>" : ""))
                .collect(Collectors.toList());
        List<String> optional = streamOptions(Option::isOptional)
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

    public static class CommandLineException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CommandLineException(String _message) {
            super(_message);
        }

        public CommandLineException(String _message, Throwable _cause) {
            super(_message, _cause);
        }
    }

    /**
     * Describes a command-line option.<br>
     * Options are created using the associated {@link Builder}.
     *
     * @param <T> data type of the option
     * @since 2.1.9 - 2022-04-19
     */
    public static final class Option<T> {

        /** Name of the option. */
        private final String   name;

        /** The data type of this option. */
        private final Class<T> dataType;

        /** Whether this option is required. */
        private final boolean  required;

        /** Whether this option's value is optional. */
        private final boolean  hasValue;

        /** Default value. */
        private final T        defaultValue;

        /** Optional description of this option. */
        private final String   description;

        private Option(Option.Builder<T> _builder) {
            name = _builder.name;
            dataType = _builder.dataType;
            required = _builder.required;
            hasValue = _builder.hasValue;
            defaultValue = _builder.defaultValue;
            description = _builder.description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public boolean isOptional() {
            return !required;
        }

        public boolean hasValue() {
            return hasValue;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public Class<?> getDataType() {
            return dataType;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + String.format("[%s, dataType=%s, required=%s, hasValue=%s, default=%s, descr=%s]",
                            name, Optional.ofNullable(dataType).map(Class::getName).orElse(null), required, hasValue, defaultValue, description);
        }

        /**
         * Returns a builder for a new Option with the specified data type.<br>
         * As the data type is specified, the option must have a value (and may have a default value).
         *
         * @param <T> data type of the option
         * @param _dataType data type of option
         * @return builder
         */
        public static <T> Option.Builder<T> builder(Class<T> _dataType) {
            return new Option.Builder<>(_dataType);
        }

        /**
         * Returns a builder for a new Option without value (therefore default value not permitted).
         *
         * @return builder
         */
        public static Option.Builder<Void> builder() {
            return new Option.Builder<>(null);
        }

        static void throwIf(boolean _condition, String _error) {
            if (_condition) {
                throw new CommandLineException(_error);
            }
        }

        /**
         * Builder for a command-line option.<br>
         * The builder guarantees the option it builds is valid.<br>
         * At a minimum an option requires a name.
         */
        public static final class Builder<T> {

            private String         name;
            private final Class<T> dataType;
            private boolean        required;
            private final boolean  hasValue;
            private T              defaultValue;
            private String         description;

            private Builder(Class<T> _dataType) {
                dataType = _dataType;
                hasValue = _dataType != null;
            }

            public Option.Builder<T> name(String _name) {
                return apply(() -> name = _name);
            }

            public Option.Builder<T> required(boolean _required) {
                return apply(() -> required = _required);
            }

            public Option.Builder<T> required() {
                return required(true);
            }

            public Option.Builder<T> optional() {
                return required(false);
            }

            public Option.Builder<T> defaultValue(T _defaultValue) {
                throwIf(!hasValue && _defaultValue != null, "Option cannot have a default value");
                return apply(() -> defaultValue = _defaultValue);
            }

            public Option.Builder<T> description(String _description) {
                return apply(() -> description = _description);
            }

            public Option<T> build() {
                throwIf(name == null || name.isBlank(), "Option requires a name");
                return new Option<>(this);
            }

            private Option.Builder<T> apply(Runnable _r) {
                _r.run();
                return this;
            }

        }
    }

}

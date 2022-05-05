package com.github.hypfvieh.cli.parser;

import static com.github.hypfvieh.cli.parser.StaticUtils.requireOption;
import static com.github.hypfvieh.cli.parser.StaticUtils.requireParsed;
import static com.github.hypfvieh.cli.parser.StaticUtils.requireUniqueOption;
import static com.github.hypfvieh.cli.parser.StaticUtils.uncheckedCast;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.cli.parser.converter.DoubleConverter;
import com.github.hypfvieh.cli.parser.converter.IValueConverter;
import com.github.hypfvieh.cli.parser.converter.LocalDateConverter;
import com.github.hypfvieh.cli.parser.converter.LocalDateTimeConverter;
import com.github.hypfvieh.cli.parser.converter.LocalTimeConverter;
import com.github.hypfvieh.cli.parser.formatter.DefaultUsageFormatter;
import com.github.hypfvieh.cli.parser.formatter.IUsageFormatter;

/**
 * Base class of every command line.
 * 
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 *
 * @param <B> concrete command line implementation
 */
public abstract class AbstractBaseCommandLine<B extends AbstractBaseCommandLine<?>> {
    
    private final Logger                      logger             = LoggerFactory.getLogger(getClass());

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
    
    public AbstractBaseCommandLine() {
        registerDefaultConverters();
        withLongOptPrefix("--");
        withShortOptPrefix("-");
    }
    
    protected abstract B self();

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
    }
    
    
    B reset() {
        return accessSync(t -> {
            getArgBundle().unknownTokens().clear();
            Stream.of(getArgBundle().knownArgs(), getArgBundle().unknownArgs(), getArgBundle().dupArgs()).forEach(Map::clear);
            return t;
        });
    }

    public <T> B registerConverter(Class<T> _type, IValueConverter<T> _converter) {
        Objects.requireNonNull(_type, "Type required");
        Objects.requireNonNull(_converter, "Converter required");
        argBundle.converters().put(_type, _converter);
        if (_type.isPrimitive()) {
            return registerConverter(uncheckedCast((Class<?>) MethodType.methodType(_type).wrap().returnType()),
                    _converter);
        }
        return self();
    }
    
    protected void setParsed(boolean _b) {
        parsed.set(_b);
    }

    public B addOption(CmdArgOption<?> _option) {
        requireOption(_option);
        requireUniqueOption(_option, this);
        
        if (_option.getName() != null) {
            getArgBundle().options().put(_option.getName(), _option);    
        }
        if (_option.getShortName() != null) {
            getArgBundle().options().put(_option.getShortName(), _option);
        }
        
        getLogger().debug("Added {} command-line option '{}': {}",
                _option.isRequired() ? "required" : "optional", _option.getName(), _option.getDescription());
        return self();
    }

    public B addOptions(CmdArgOption<?>... _options) {
        if (_options != null) {
            for (CmdArgOption<?> option : _options) {
                addOption(option);
            }
        }
        return self();
    }
    
    boolean hasOption(CharSequence _optionName) {
        return getOption(_optionName) != null;
    }

    public boolean hasOption(CmdArgOption<?> _option) {
        return getOption(requireOption(_option).getName()) != null;
    }

    public Map<String, CmdArgOption<?>> getOptions() {
        return accessSync(t -> Collections.unmodifiableMap(t.getArgBundle().options()));
    }

    public CmdArgOption<?> getOption(CharSequence _optionName) {
        return accessSync(t -> getArgBundle().options().get(Objects.requireNonNull(_optionName, "Option name required")));
    }

    public Map<CmdArgOption<?>, String> getKnownArgs() {
        return accessSync(t -> Collections.unmodifiableMap(requireParsed(t).getArgBundle().knownArgs()));
    }

    public Map<String, String> getUnknownArgs() {
        return accessSync(t -> requireParsed(t).getArgBundle().unknownArgs());
    }

    public List<String> getUnknownTokens() {
        return accessSync(t -> requireParsed(t).getArgBundle().unknownTokens());
    }

    public Map<CmdArgOption<?>, String> getDupArgs() {
        return accessSync(t -> requireParsed(t).getArgBundle().dupArgs());
    }
    
    public void printUsage() {
        printUsage(null, System.out);
    }
    
    public void printUsage(String _mainClassName, OutputStream _output) {
        try (PrintWriter pw = new PrintWriter(_output)) {
            pw.print(getUsage(_mainClassName));
            pw.flush();
        }
    }

    public String getUsage(String _mainClassName) {
        return usageFormatter.format(new ArrayList<>(getOptions().values()), _mainClassName);
    }
    
    public B withUsageFormatter(IUsageFormatter _formatter) {
        usageFormatter = Objects.requireNonNull(_formatter, "Formatter required");
        return self();
    }
    
    public B withFailOnUnknownArg(boolean _failOnUnknownArg) {
        failOnUnknownArg = _failOnUnknownArg;
        return self();
    }

    public B withFailOnUnknownToken(boolean _failOnUnknownToken) {
        failOnUnknownToken = _failOnUnknownToken;
        return self();
    }

    public B withFailOnDupArg(boolean _failOnDupArg) {
        failOnDupArg = _failOnDupArg;
        return self();
    }

    public B withShortOptPrefix(String _prefix) {
        shortOptPrefix = _prefix;
        shortOptPattern = Pattern.compile("^(?:" + Pattern.quote(_prefix) + "(.+))");
        return self();
    }

    public B withLongOptPrefix(String _prefix) {
        longOptPrefix = _prefix;
        longOptPattern = Pattern.compile("^(?:" + Pattern.quote(_prefix) + "(.+))");
        return self();
    }

    public B setExceptionType(Class<? extends RuntimeException> _exceptionType) {
        Objects.requireNonNull(_exceptionType, "Exception type required");
        try {
            _exceptionType.getConstructor(String.class);
        } catch (NoSuchMethodException | SecurityException _ex) {
            throw new CommandLineException("Exception type requires a single-argument constructor of type String");
        }
        exceptionType = _exceptionType;
        return self();
    }
    
    public String getShortOptPrefix() {
        return shortOptPrefix;
    }

    public String getLongOptPrefix() {
        return longOptPrefix;
    }

    protected Pattern getLongOptPattern() {
        return longOptPattern;
    }

    protected Pattern getShortOptPattern() {
        return shortOptPattern;
    }

    public Class<? extends RuntimeException> getExceptionType() {
        return exceptionType;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected ArgumentBundle getArgBundle() {
        return argBundle;
    }

    protected boolean isParsed() {
        return parsed.get();
    }
    
    public boolean isFailOnDupArg() {
        return failOnDupArg;
    }

    public boolean isFailOnUnknownArg() {
        return failOnUnknownArg;
    }

    public boolean isFailOnUnknownToken() {
        return failOnUnknownToken;
    }

    private <R> R accessSync(Function<B, R> _function) {
        Objects.requireNonNull(_function, "Function required");
        synchronized (self()) {
            return _function.apply(self());
        }
    }
}

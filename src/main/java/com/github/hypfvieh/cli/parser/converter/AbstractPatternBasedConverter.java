package com.github.hypfvieh.cli.parser.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base converter using a list of patterns to find a suitable convert operation.
 * <p>
 * Allows adding additional number patterns by using {@link #addPattern(P)}.<br>
 * Will use the first pattern which successfully parsed the input string.
 * </p>
 * 
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public abstract class AbstractPatternBasedConverter<T, P> implements IValueConverter<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final List<P> patterns = new ArrayList<>();
    
    public void addPattern(P _pattern) {
        patterns.add(Objects.requireNonNull(_pattern, "Pattern required"));
    }

    protected Logger getLogger() {
        return logger;
    }

    protected List<P> getPatterns() {
        return patterns;
    }
}
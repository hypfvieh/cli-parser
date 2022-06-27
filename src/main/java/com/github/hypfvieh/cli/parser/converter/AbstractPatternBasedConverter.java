package com.github.hypfvieh.cli.parser.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base converter using a list of patterns to find a suitable convert operation.
 * <p>
 * Allows adding additional number patterns by using {@link #addPattern(P)}.<br>
 * Will use the first pattern which successfully parsed the input string.
 * </p>
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public abstract class AbstractPatternBasedConverter<T, P> implements IValueConverter<T> {
    private final Logger  logger   = LoggerFactory.getLogger(getClass());

    private final List<P> patterns = new ArrayList<>();

    public void addPattern(P _pattern) {
        // always put custom patterns in front to try it first
        patterns.add(0, Objects.requireNonNull(_pattern, "Pattern required"));
    }

    protected Logger getLogger() {
        return logger;
    }

    protected List<P> getPatterns() {
        return patterns;
    }
}

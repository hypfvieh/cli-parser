package com.github.hypfvieh.cli.parser.converter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.github.hypfvieh.cli.parser.CommandLineException;

/**
 * Converts a string to a {@link Double} object.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public class DoubleConverter extends AbstractPatternBasedConverter<Double, NumberFormat> {

    public DoubleConverter() {
        addPattern(DecimalFormat.getInstance());
        addPattern(DecimalFormat.getInstance(Locale.US));
    }

    @Override
    public Double convert(String _string) {
        try {
            return Double.parseDouble(_string);
        } catch (NumberFormatException _ex) {
            LoggerFactory.getLogger(getClass()).trace("Unable to parse number input '{}' with parseDouble function", _string);
        }

        for (NumberFormat nf : getPatterns()) {
            try {
                return nf.parse(_string).doubleValue();
            } catch (ParseException _ex) {
                getLogger().trace("Unable to parse number input '{}' with parser '{}'", _string, nf);
            }
        }

        throw new CommandLineException("Unable to parse input '" + _string + "' as double");
    }

}

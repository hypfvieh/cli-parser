package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.CommandLineException;

import java.lang.System.Logger.Level;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

/**
 * Converts a string to a {@link LocalTime} object.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public class LocalTimeConverter extends AbstractPatternBasedConverter<LocalTime, DateTimeFormatter> {

    /**
     * Default constructor.
     */
    public LocalTimeConverter() {
        addPattern(DateTimeFormatter.ISO_LOCAL_TIME);
        addPattern(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        addPattern(DateTimeFormatter.ofPattern("HH:mm:ss"));
        addPattern(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        addPattern(DateTimeFormatter.ofPattern("HHmmss"));
        addPattern(DateTimeFormatter.ofPattern("HHmm"));
    }

    @Override
    public LocalTime convert(String _string) {
        for (DateTimeFormatter dtf : getPatterns()) {
            try {
                return LocalTime.parse(_string, dtf);
            } catch (DateTimeParseException _ex) {
                getLogger().log(Level.TRACE, "Unable to parse time input ''{0}'' with parser ''{1}''", _string, dtf);
            }
        }

        throw new CommandLineException("Unable to parse input '" + _string + "' as time");
    }

}

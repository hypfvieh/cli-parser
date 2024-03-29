package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.CommandLineException;

import java.lang.System.Logger.Level;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

/**
 * Converts a string to a {@link LocalDateTime} object.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public class LocalDateTimeConverter extends AbstractPatternBasedConverter<LocalDateTime, DateTimeFormatter> {

    /**
     * Default constructor.
     */
    public LocalDateTimeConverter() {
        addPattern(DateTimeFormatter.BASIC_ISO_DATE);
        addPattern(DateTimeFormatter.ISO_DATE_TIME);
        addPattern(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        addPattern(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        addPattern(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        addPattern(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    @Override
    public LocalDateTime convert(String _string) {
        for (DateTimeFormatter dtf : getPatterns()) {
            try {
                return LocalDateTime.parse(_string, dtf);
            } catch (DateTimeParseException _ex) {
                getLogger().log(Level.TRACE, "Unable to parse datetime input ''{0}'' with parser ''{1}''", _string, dtf);
            }
        }

        throw new CommandLineException("Unable to parse input '" + _string + "' as datetime");
    }

}

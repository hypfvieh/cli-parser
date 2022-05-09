package com.github.hypfvieh.cli.parser.converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import com.github.hypfvieh.cli.parser.CommandLineException;

/**
 * Converts a string to a {@link LocalDateTime} object.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public class LocalDateTimeConverter extends AbstractPatternBasedConverter<LocalDateTime, DateTimeFormatter> {

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
                getLogger().trace("Unable to parse datetime input '{}' with parser '{}'", _string, dtf);
            }
        }

        throw new CommandLineException("Unable to parse input '" + _string + "' as datetime");
    }

}

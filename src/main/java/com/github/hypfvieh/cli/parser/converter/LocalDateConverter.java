package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.CommandLineException;

import java.lang.System.Logger.Level;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

/**
 * Converts a string to a {@link LocalDate} object.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public class LocalDateConverter extends AbstractPatternBasedConverter<LocalDate, DateTimeFormatter> {

    /**
     * Default constructor.
     */
    public LocalDateConverter() {
        addPattern(DateTimeFormatter.BASIC_ISO_DATE);
        addPattern(DateTimeFormatter.ISO_DATE);
        addPattern(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
    }

    @Override
    public LocalDate convert(String _string) {
        for (DateTimeFormatter dtf : getPatterns()) {
            try {
                return LocalDate.parse(_string, dtf);
            } catch (DateTimeParseException _ex) {
                getLogger().log(Level.TRACE, "Unable to parse date input ''{0}'' with parser ''{1}''", _string, dtf);
            }
        }

        throw new CommandLineException("Unable to parse input '" + _string + "' as date");
    }

}

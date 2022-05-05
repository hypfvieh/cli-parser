package com.github.hypfvieh.cli.parser.converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import org.slf4j.LoggerFactory;

import com.github.hypfvieh.cli.parser.CommandLineException;

/**
 * Converts a string to a {@link LocalDate} object.
 * <p>
 * Allows adding additional date patterns by using {@link #addPattern(DateTimeFormatter)}.<br>
 * </p>
 *  
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public class LocalDateConverter extends AbstractPatternBasedConverter<LocalDate, DateTimeFormatter> {

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
                LoggerFactory.getLogger(getClass()).trace("Unable to parse date input '{}' with parser '{}'", _string, dtf);
            }
        } 
        
        throw new CommandLineException("Unable to parse input '" + _string + "' as date");
    }

}

package com.github.hypfvieh.cli.parser.converter;

/**
 * Interface implemented by all value converters to convert a given String to the proper object type.
 * 
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 * 
 * @param <T> type of result object
 */
@FunctionalInterface
public interface IValueConverter<T> {
    /**
     * Called to convert command line argument String to specified type.
     * 
     * @param _string input string, maybe empty/blank, never null
     * @return converted value, never null
     * 
     * @throws RuntimeException (e.g. CommandLineException) when parsing fails
     */
    T convert(String _string);
}

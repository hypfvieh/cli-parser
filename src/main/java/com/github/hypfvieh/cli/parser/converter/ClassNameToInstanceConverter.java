package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.CommandLineException;

import java.lang.reflect.InvocationTargetException;

/**
 * Converts a fully qualified class name to an instance of that class by invoking its default constructor.
 *
 * @since 1.0.1 - 2022-06-29
 */
public class ClassNameToInstanceConverter<T> implements IValueConverter<T> {

    @Override
    public T convert(String _str) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) Class.forName(_str);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException _ex) {
            throw new CommandLineException("Unable to create instance of class '" + _str + "'", _ex);
        }
    }

}

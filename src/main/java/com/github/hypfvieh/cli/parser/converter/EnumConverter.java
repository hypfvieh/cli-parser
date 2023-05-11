package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.CommandLineException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts a string to an {@link Enum} constant of the type specified in the constructor
 * and the same case-insensitive name.
 *
 * @since 1.0.4 - 2023-05-11
 */
public final class EnumConverter implements IValueConverter<Enum<?>> {
    private final Class<Enum<?>> enumType;

    public EnumConverter(Class<Enum<?>> _enumType) {
        enumType = Objects.requireNonNull(_enumType, "Enum type required");
    }

    @Override
    public Enum<?> convert(String _str) {
        return Optional.ofNullable(_str == null || _str.isBlank() ? null
            : Arrays.stream(enumType.getEnumConstants())
                .filter(c -> c.name().equalsIgnoreCase(_str))
                .findFirst()
                .map(Enum.class::cast)
                .orElse(null))
            .orElseThrow(() -> new CommandLineException("'" + _str + "' is not a known value of enum " + enumType.getSimpleName()));
    }

}

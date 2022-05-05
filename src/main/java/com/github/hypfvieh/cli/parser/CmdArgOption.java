package com.github.hypfvieh.cli.parser;

import java.util.Optional;

/**
 * Describes a command-line option.<br>
 * Options are created using the associated {@link Builder}.
 *
 * @param <T> data type of the option
 * @since 1.0.0 - 2022-04-19
 */
public final class CmdArgOption<T> {

    /** Name of the option. */
    private final String   name;
    
    /** Short name of the option. */
    private final String   shortName;

    /** The data type of this option. */
    private final Class<T> dataType;

    /** Whether this option is required. */
    private final boolean  required;

    /** Whether this option's value is optional. */
    private final boolean  hasValue;

    /** Whether this option can be repeated multiple times. */
    private final boolean  repeatable;

    /** Default value. */
    private final T        defaultValue;

    /** Optional description of this option. */
    private final String   description;

    private CmdArgOption(CmdArgOption.Builder<T> _builder) {
        name = _builder.name;
        shortName = _builder.shortName;
        dataType = _builder.dataType;
        required = _builder.required;
        hasValue = _builder.hasValue;
        defaultValue = _builder.defaultValue;
        description = _builder.description;
        repeatable = _builder.repeatable;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public boolean isOptional() {
        return !required;
    }

    public boolean hasValue() {
        return hasValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + String.format("[%s/%s, dataType=%s, required=%s, hasValue=%s, default=%s, descr=%s]",
                        name, shortName, Optional.ofNullable(dataType).map(Class::getName).orElse(null), required, hasValue, defaultValue, description);
    }

    /**
     * Returns a builder for a new Option with the specified data type.<br>
     * As the data type is specified, the option must have a value (and may have a default value).
     *
     * @param <T> data type of the option
     * @param _dataType data type of option
     * @return builder
     */
    public static <T> CmdArgOption.Builder<T> builder(Class<T> _dataType) {
        return new CmdArgOption.Builder<>(_dataType);
    }

    /**
     * Returns a builder for a new Option without value (therefore default value not permitted).
     *
     * @return builder
     */
    public static CmdArgOption.Builder<Void> builder() {
        return new CmdArgOption.Builder<>(null);
    }

    static void throwIf(boolean _condition, String _error) {
        if (_condition) {
            throw new CommandLineException(_error);
        }
    }

    /**
     * Builder for a command-line option.<br>
     * The builder guarantees the option it builds is valid.<br>
     * At a minimum an option requires a name.
     */
    public static final class Builder<T> {

        private String         name;
        private String         shortName;
        private final Class<T> dataType;
        private boolean        required;
        private final boolean  hasValue;
        private boolean        repeatable;
        private T              defaultValue;
        private String         description;

        private Builder(Class<T> _dataType) {
            dataType = _dataType;
            hasValue = _dataType != null;
        }

        public CmdArgOption.Builder<T> name(String _name) {
            return apply(() -> name = _name);
        }

        public CmdArgOption.Builder<T> shortName(String _name) {
            return apply(() -> shortName = _name);
        }

        public CmdArgOption.Builder<T> required(boolean _required) {
            return apply(() -> required = _required);
        }
        
        public CmdArgOption.Builder<T> repeatable(boolean _repeat) {
            return apply(() -> repeatable = _repeat);
        }

        public CmdArgOption.Builder<T> repeatable() {
            return apply(() -> repeatable = true);
        }

        public CmdArgOption.Builder<T> required() {
            return required(true);
        }

        public CmdArgOption.Builder<T> optional() {
            return required(false);
        }

        public CmdArgOption.Builder<T> defaultValue(T _defaultValue) {
            throwIf(!hasValue && _defaultValue != null, "Option cannot have a default value");
            return apply(() -> defaultValue = _defaultValue);
        }

        public CmdArgOption.Builder<T> description(String _description) {
            return apply(() -> description = _description);
        }

        public CmdArgOption<T> build() {
            
            throwIf((name == null || name.isBlank()) && (shortName == null || shortName.isBlank()), "Option requires a name or shortname");
            return new CmdArgOption<>(this);
        }

        private CmdArgOption.Builder<T> apply(Runnable _r) {
            _r.run();
            return this;
        }

    }
}
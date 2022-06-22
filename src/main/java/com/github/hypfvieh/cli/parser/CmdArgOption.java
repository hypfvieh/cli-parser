package com.github.hypfvieh.cli.parser;

import java.util.Objects;
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
    private final String    name;

    /** Short name of the option. */
    private final Character shortName;

    /** The data type of this option. */
    private final Class<T>  dataType;

    /** Whether this option is required. */
    private final boolean   required;

    /** Whether this option's value is optional. */
    private final boolean   hasValue;

    /** Whether this option can be repeated multiple times. */
    private final boolean   repeatable;

    /** Default value. */
    private final T         defaultValue;

    /** Optional description of this option. */
    private final String    description;

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

    /**
     * Returns the long name of this option
     * @return String, maybe null or empty
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the short name of this option.
     * @return String, maybe empty or null
     */
    public String getShortName() {
        return shortName == null ? null : String.valueOf(shortName);
    }

    /**
     * Returns the description text for this option.
     * @return String, maybe empty or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Flag to signal if this option is required.
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Flag to allow the option to be repeated multiple times.
     * @return true if repeatable
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * Flag to signal that this option is optional.
     * @return true if optional
     */
    public boolean isOptional() {
        return !required;
    }

    /**
     * Flag to signal if the option requires a value.
     * @return true if value required
     */
    public boolean hasValue() {
        return hasValue;
    }

    /**
     * Returns the default value for this option (when option was not set).
     * @return default value, maybe null
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the type of data to create from argument.
     * @return class, maybe null
     */
    public Class<?> getDataType() {
        return dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, shortName, dataType, required, hasValue, repeatable, defaultValue);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        } else if (_obj == null || getClass() != _obj.getClass()) {
            return false;
        }
        CmdArgOption<?> other = (CmdArgOption<?>) _obj;
        return Objects.equals(name, other.name)
                && Objects.equals(shortName, other.shortName)
                && dataType == other.dataType
                && required == other.required
                && hasValue == other.hasValue
                && repeatable == other.repeatable
                && Objects.equals(defaultValue, other.defaultValue);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + String.format("[%s/%s, dataType=%s, required=%s, repeatable=%s, hasValue=%s, default=%s, descr=%s]",
                        name, shortName, Optional.ofNullable(dataType).map(Class::getName).orElse(null),
                        required, repeatable, hasValue, defaultValue, description);
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

    /**
     * Throws a {@link CommandLineException} if condition validates to true.
     *
     * @param _condition condition
     * @param _error error message text
     *
     * @throws CommandLineException when condition is true
     */
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
        private Character      shortName;
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

        /**
         * Sets option long name.
         * @param _name name
         *
         * @return this
         */
        public CmdArgOption.Builder<T> name(String _name) {
            return apply(() -> name = _name);
        }

        /**
         * Sets option short name.
         * @param _name name
         *
         * @return this
         */
        public CmdArgOption.Builder<T> shortName(Character _name) {
            return apply(() -> shortName = _name);
        }

        /**
         * Sets option to be required or optional.
         * @param _required true to be required
         *
         * @return this
         */
        public CmdArgOption.Builder<T> required(boolean _required) {
            return apply(() -> required = _required);
        }

        /**
         * Sets option to be required.
         *
         * @return this
         */
        public CmdArgOption.Builder<T> required() {
            return required(true);
        }

        /**
         * Sets option to be optional.
         *
         * @return this
         */
        public CmdArgOption.Builder<T> optional() {
            return required(false);
        }

        /**
         * Sets option to be repeatable ({@code true}) or not ({@code false}).
         * @param _repeatable true to be repeatable
         *
         * @return this
         */
        public CmdArgOption.Builder<T> repeatable(boolean _repeatable) {
            return apply(() -> repeatable = _repeatable);
        }

        /**
         * Sets option to be repeatable.
         *
         * @return this
         */
        public CmdArgOption.Builder<T> repeatable() {
            return repeatable(true);
        }

        /**
         * Sets the option's default value.
         * @param _defaultValue value to use, never null
         *
         * @return this
         */
        public CmdArgOption.Builder<T> defaultValue(T _defaultValue) {
            throwIf(!hasValue && _defaultValue != null, "Option cannot have a default value");
            return apply(() -> defaultValue = _defaultValue);
        }

        /**
         * Sets the option's description text.
         * @param _description text to use
         *
         * @return this
         */
        public CmdArgOption.Builder<T> description(String _description) {
            return apply(() -> description = _description);
        }

        /**
         * Create the option object based on configuration.
         * @return CmdArgOption
         */
        public CmdArgOption<T> build() {
            throwIf((name == null || name.isBlank()) && (shortName == null || shortName == ' '), "Option requires a name or shortname");
            return new CmdArgOption<>(this);
        }

        /**
         * Allows creating invalid options.
         * Intended to be used for testing only.
         *
         * @return CmdArgOption
         */
        CmdArgOption<T> buildInvalid() {
            return new CmdArgOption<>(this);
        }

        /**
         * Execute action on this builder.
         *
         * @param _r action to perform
         *
         * @return this
         */
        private CmdArgOption.Builder<T> apply(Runnable _r) {
            _r.run();
            return this;
        }

    }
}

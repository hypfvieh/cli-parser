package com.github.hypfvieh.cli.parser;

import com.github.hypfvieh.cli.parser.converter.IValueConverter;

import java.util.*;

/**
 * Bundles different argument-specific information.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public final class ArgumentBundle {
    private final Map<CmdArgOption<?>, String>       knownArgs      = new LinkedHashMap<>();
    private final Map<CmdArgOption<?>, List<String>> knownMultiArgs = new LinkedHashMap<>();
    private final Map<String, String>                unknownArgs    = new LinkedHashMap<>();
    private final List<String>                       unknownTokens  = new ArrayList<>();
    private final Map<CmdArgOption<?>, String>       dupArgs        = new LinkedHashMap<>();
    private final List<CmdArgOption<?>>              missingArgs    = new ArrayList<>();
    private final Map<String, CmdArgOption<?>>       options        = new LinkedHashMap<>();
    private final Map<Class<?>, IValueConverter<?>>  converters     = new LinkedHashMap<>();

    Map<CmdArgOption<?>, String> getKnownArgs() {
        return knownArgs;
    }

    Map<CmdArgOption<?>, List<String>> getKnownMultiArgs() {
        return knownMultiArgs;
    }

    Map<String, String> getUnknownArgs() {
        return unknownArgs;
    }

    List<String> getUnknownTokens() {
        return unknownTokens;
    }

    Map<CmdArgOption<?>, String> getDupArgs() {
        return dupArgs;
    }

    List<CmdArgOption<?>> getMissingArgs() {
        return missingArgs;
    }

    Map<String, CmdArgOption<?>> getOptions() {
        return options;
    }

    /**
     * Looks up an option by name name or short name of the specified option object.
     *
     * @param _option option to lookup, may not be null
     * @return option if found, null otherwise
     */
    CmdArgOption<?> getOption(CmdArgOption<?> _option) {
        StaticUtils.requireOption(_option);

        CmdArgOption<?> option = Optional.ofNullable(_option.getName()).map(options::get).orElse(null);
        if (option == null) {
            option = Optional.ofNullable(_option.getShortName()).map(options::get).orElse(null);
        }

        return option;
    }

    Map<Class<?>, IValueConverter<?>> getConverters() {
        return converters;
    }

}

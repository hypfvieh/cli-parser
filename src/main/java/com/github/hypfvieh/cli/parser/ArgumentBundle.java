package com.github.hypfvieh.cli.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.hypfvieh.cli.parser.converter.IValueConverter;

/**
 * Bundles different argument specific information.
 *
 * @author hypfvieh
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

    public Map<CmdArgOption<?>, String> getKnownArgs() {
        return knownArgs;
    }

    public Map<CmdArgOption<?>, List<String>> getKnownMultiArgs() {
        return knownMultiArgs;
    }

    public Map<String, String> getUnknownArgs() {
        return unknownArgs;
    }

    public List<String> getUnknownTokens() {
        return unknownTokens;
    }

    public Map<CmdArgOption<?>, String> getDupArgs() {
        return dupArgs;
    }

    public List<CmdArgOption<?>> getMissingArgs() {
        return missingArgs;
    }

    public Map<String, CmdArgOption<?>> getOptions() {
        return options;
    }

    public Map<Class<?>, IValueConverter<?>> getConverters() {
        return converters;
    }

}

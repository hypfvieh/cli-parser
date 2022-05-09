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

    public Map<CmdArgOption<?>, String> knownArgs() {
        return knownArgs;
    }

    public Map<CmdArgOption<?>, List<String>> knownMultiArgs() {
        return knownMultiArgs;
    }

    public Map<String, String> unknownArgs() {
        return unknownArgs;
    }

    public List<String> unknownTokens() {
        return unknownTokens;
    }

    public Map<CmdArgOption<?>, String> dupArgs() {
        return dupArgs;
    }

    public List<CmdArgOption<?>> missingArgs() {
        return missingArgs;
    }

    public Map<String, CmdArgOption<?>> options() {
        return options;
    }

    public Map<Class<?>, IValueConverter<?>> converters() {
        return converters;
    }

}

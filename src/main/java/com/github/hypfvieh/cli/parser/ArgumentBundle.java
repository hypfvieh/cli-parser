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
public record ArgumentBundle(
        Map<CmdArgOption<?>, String>       knownArgs,      
        Map<CmdArgOption<?>, List<String>> knownMultiArgs, 
        Map<String, String>                unknownArgs,    
        List<String>                       unknownTokens,  
        Map<CmdArgOption<?>, String>       dupArgs,        
        List<CmdArgOption<?>>              missingArgs,
        Map<String, CmdArgOption<?>>       options,
        Map<Class<?>, IValueConverter<?>> converters
        ) {
    
    public ArgumentBundle() {
        this(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), 
                new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>(), 
                new LinkedHashMap<>(), new LinkedHashMap<>());
    }

}

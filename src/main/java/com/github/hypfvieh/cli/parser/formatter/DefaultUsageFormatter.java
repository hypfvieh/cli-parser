package com.github.hypfvieh.cli.parser.formatter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.hypfvieh.cli.parser.CmdArgOption;
import com.github.hypfvieh.cli.parser.StaticUtils;

/**
 * Default usage formatter used when no other formatter was specified
 *  
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public class DefaultUsageFormatter implements IUsageFormatter {
    
    public String format(List<CmdArgOption<?>> _options, String _longOptPrefix, String _shortOptPrefix, String _mainClassName) {
        List<String> required = null;
        List<String> optional = null;
        
        if (_options != null) {
            required = _options.stream().filter(CmdArgOption::isRequired)
                    .map(o -> StaticUtils.formatOption(o, _longOptPrefix, _shortOptPrefix) + (o.hasValue() ? " <arg>" : ""))
                    .collect(Collectors.toList());
            
            optional = _options.stream().filter(CmdArgOption::isOptional)
                    .map(o -> StaticUtils.formatOption(o, _longOptPrefix, _shortOptPrefix) + (o.hasValue() ? " <arg>" : ""))
                    .collect(Collectors.toList());
        }
        
        StringBuilder sb = new StringBuilder()
                .append("usage: " + Optional.ofNullable(_mainClassName).orElseGet(IUsageFormatter::getMainClassName));
        if (required != null && !required.isEmpty()) {
            sb.append(" " + String.join(" ", required));
        }
        if (optional != null && !optional.isEmpty()) {
            sb.append(" [" + String.join(" ", optional) + "]");
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

}

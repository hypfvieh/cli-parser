package com.github.hypfvieh.cli.parser.formatter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.hypfvieh.cli.parser.CmdArgOption;

/**
 * Default usage formatter used when no other formatter was specified
 *  
 * @author hypfvieh
 * @since 1.0.0 - 2022-05-05
 */
public class DefaultUsageFormatter implements IUsageFormatter {
    
    public String format(List<CmdArgOption<?>> _options, String _mainClassName) {
        List<String> required = _options.stream().filter(CmdArgOption::isRequired)
                .map(o -> "--" + o.getName() + (o.hasValue() ? " <arg>" : ""))
                .collect(Collectors.toList());
        List<String> optional = _options.stream().filter(CmdArgOption::isOptional)
                .map(o -> "--" + o.getName() + (o.hasValue() ? " <arg>" : ""))
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder()
                .append("usage: " + Optional.ofNullable(_mainClassName).orElseGet(IUsageFormatter::getMainClassName));
        if (!required.isEmpty()) {
            sb.append(" " + String.join(" ", required));
        }
        if (!optional.isEmpty()) {
            sb.append(" [" + String.join(" ", optional) + "]");
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}

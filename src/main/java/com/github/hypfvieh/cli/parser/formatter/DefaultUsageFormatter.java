package com.github.hypfvieh.cli.parser.formatter;

import com.github.hypfvieh.cli.parser.CmdArgOption;
import com.github.hypfvieh.cli.parser.StaticUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default usage formatter used when no other formatter was specified
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
public class DefaultUsageFormatter implements IUsageFormatter {

    @Override
    public String format(List<CmdArgOption<?>> _options, String _longOptPrefix, String _shortOptPrefix, String _mainClassName) {
        List<String> requiredOptions = List.of();
        List<String> optionalOptions = List.of();

        if (_options != null) {
            requiredOptions = _options.stream().filter(CmdArgOption::isRequired)
                .map(o -> StaticUtils.formatOption(o, _longOptPrefix, _shortOptPrefix) + getArgNameOrDefault(o))
                .collect(Collectors.toList());

            optionalOptions = _options.stream().filter(CmdArgOption::isOptional)
                .map(o -> StaticUtils.formatOption(o, _longOptPrefix, _shortOptPrefix) + getArgNameOrDefault(o))
                .collect(Collectors.toList());
        }

        StringBuilder sb = new StringBuilder("usage: ")
            .append(Optional.ofNullable(_mainClassName).orElseGet(IUsageFormatter::getMainClassName));
        if (!requiredOptions.isEmpty()) {
            sb.append(" ").append(String.join(" ", requiredOptions));
        }
        if (!optionalOptions.isEmpty()) {
            sb.append(" [").append(String.join(" ", optionalOptions)).append("]");
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private String getArgNameOrDefault(CmdArgOption<?> _opt) {
        if (_opt.hasValue()) {
            if (_opt.getPossibleValues().isEmpty()) {
                return " <arg>";
            } else if (_opt.getPossibleValues().size() == 1) {
                return " <" + _opt.getPossibleValues().keySet().iterator().next() + ">";
            } else {
                return " <(" + _opt.getPossibleValues().keySet().stream().map(String::valueOf).collect(Collectors.joining("|")) + ")>";
            }
        } else {
            return "";
        }
    }

}

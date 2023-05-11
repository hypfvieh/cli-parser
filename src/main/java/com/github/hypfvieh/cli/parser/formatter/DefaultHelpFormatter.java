package com.github.hypfvieh.cli.parser.formatter;

import com.github.hypfvieh.cli.parser.CmdArgOption;
import com.github.hypfvieh.cli.parser.StaticUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultHelpFormatter implements IUsageFormatter {

    /**
     * Formats the given options as proper help-text. <br>
     * Given _mainClassName is not used and can be null.
     */
    @Override
    public String format(List<CmdArgOption<?>> _options, String _longOptPrefix, String _shortOptPrefix, String _mainClassName) {

        int longestOption = _options.stream().filter(Objects::nonNull).mapToInt(o -> StaticUtils.formatOption(o, _longOptPrefix, _shortOptPrefix, ", ").length()).max().orElse(5);

        String cmdFormat = "%-" + longestOption + "s   %s";
        String possValFormat = "%-" + longestOption + "s      '%s': %s";

        List<String> lines = new ArrayList<>();
        for (CmdArgOption<?> cmdArgOption : _options) {
            String description = cmdArgOption.getDescription();

            description = handleLinebreaks(cmdFormat, description);

            lines.add(String.format(cmdFormat, StaticUtils.formatOption(cmdArgOption, _longOptPrefix, _shortOptPrefix, ", "), description));
            if (!cmdArgOption.getPossibleValues().isEmpty()) {
                for (Entry<?, String> e : cmdArgOption.getPossibleValues().entrySet()) {
                    String key = String.valueOf(e.getKey());
                    // indent is option indent + length of key + additional chars (single quotes, colon) + 6 spaces
                    String valDesc = handleLinebreaks("%-" + (longestOption + key.length() + 10) + "s%s", e.getValue());
                    lines.add(String.format(possValFormat, " ", key, valDesc));
                }
            }
        }

        return String.join(System.lineSeparator(), lines);
    }

    private String handleLinebreaks(String _format, String _text) {
        String result = _text;
        if (result.contains(System.lineSeparator())) { // take care about line breaks
            String[] split = _text.split(System.lineSeparator());
            // indent every line
            result = split[0] + System.lineSeparator() + Arrays.stream(split)
                .skip(1)
                .map(s -> String.format(_format, " ", s))
                .collect(Collectors.joining(System.lineSeparator()));
        }
        return result;
    }

}

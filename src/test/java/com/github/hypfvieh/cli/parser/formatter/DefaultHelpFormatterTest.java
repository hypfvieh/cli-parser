package com.github.hypfvieh.cli.parser.formatter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CmdArgOption;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DefaultHelpFormatterTest extends AbstractBaseTest {

    private static final String EXPECTED = "-f, --optionWithValue   Optional Option which takes a value\n"
        + "--optWithValue          Required long option which takes a value\n"
        + "-o                      Required option with only short option name and a value\n"
        + "-p, --optVal            Required repeatable option with value\n"
        + "-n, --noVal             Required Option without a value\n"
        + "                        This also has a long\n"
        + "                        description with multiple\n"
        + "                        linebreaks\n"
        + "-v, --possVal           Value with option list\n"
        + "                           'one': The First Value\n"
        + "                           'two': The Second Value\n"
        + "                                  This may have\n"
        + "                                  some sort of special\n"
        + "                                  meaning";

    @Test
    void testFormat() {
        CmdArgOption<?> opt1 = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .shortName('f')
            .optional()
            .defaultValue("def")
            .description("Optional Option which takes a value")
            .build();

        CmdArgOption<?> opt2 = CmdArgOption.builder(String.class)
            .name("optWithValue")
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("Required long option which takes a value")
            .build();

        CmdArgOption<?> opt3 = CmdArgOption.builder(String.class)
            .shortName('o')
            .required(true)
            .defaultValue("def")
            .description("Required option with only short option name and a value")
            .build();

        CmdArgOption<?> opt4 = CmdArgOption.builder(String.class)
            .name("optVal")
            .shortName('p')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("Required repeatable option with value")
            .build();

        CmdArgOption<?> opt5 = CmdArgOption.builder(null)
            .name("noVal")
            .shortName('n')
            .required(true)
            .repeatable()
            .description("Required Option without a value\nThis also has a long\ndescription with multiple\nlinebreaks")
            .build();

        Map<String, String> possVals = new LinkedHashMap<>();
        possVals.put("one", "The First Value");
        possVals.put("two", "The Second Value\nThis may have\nsome sort of special\nmeaning");
        CmdArgOption<?> opt6 = CmdArgOption.builder(String.class)
            .name("possVal")
            .shortName('v')
            .required(true)
            .possibleValue(possVals)
            .description("Value with option list")
            .build();

        List<CmdArgOption<?>> list = List.of(opt1, opt2, opt3, opt4, opt5, opt6);

        DefaultHelpFormatter dhf = new DefaultHelpFormatter();
        String format = dhf.format(list, "--", "-", "Test");

        assertEquals(EXPECTED, format);
    }

}

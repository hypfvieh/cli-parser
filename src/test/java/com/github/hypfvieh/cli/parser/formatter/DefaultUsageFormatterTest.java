package com.github.hypfvieh.cli.parser.formatter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CmdArgOption;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DefaultUsageFormatterTest extends AbstractBaseTest {

    @Test
    void testFormat() {
        CmdArgOption<?> opt1 = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .shortName('f')
            .optional()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> opt2 = CmdArgOption.builder(String.class)
            .name("optWithValue")
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> opt3 = CmdArgOption.builder(String.class)
            .shortName('o')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> opt4 = CmdArgOption.builder(String.class)
            .name("optVal")
            .shortName('p')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> opt5 = CmdArgOption.builder(null)
            .name("noVal")
            .shortName('n')
            .required(true)
            .repeatable()
            .description("descr")
            .build();

        Map<String, String> possVals = new LinkedHashMap<>();
        possVals.put("one", "foo");
        possVals.put("two", "bar");
        CmdArgOption<?> opt6 = CmdArgOption.builder(String.class)
            .name("possVal")
            .shortName('v')
            .required(true)
            .possibleValue(possVals)
            .description("descr")
            .build();

        List<CmdArgOption<?>> list = List.of(opt1, opt2, opt3, opt4, opt5, opt6);

        DefaultUsageFormatter duf = new DefaultUsageFormatter();
        assertEquals("usage: Test" + System.lineSeparator(), duf.format(null, "--", "-", "Test"));
        assertEquals("usage: Test --optWithValue <arg> -o <arg> -p/--optVal <arg> -n/--noVal -v/--possVal <(one|two)> [-f/--optionWithValue <arg>]" + System.lineSeparator(),
            duf.format(list, "--", "-", "Test"));
    }

}

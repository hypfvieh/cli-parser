package com.github.hypfvieh.cli.parser.formatter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CmdArgOption;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        List<CmdArgOption<?>> list = List.of(opt1, opt2, opt3, opt4, opt5);

        DefaultUsageFormatter duf = new DefaultUsageFormatter();
        assertEquals("usage: Test" + System.lineSeparator(), duf.format(null, "--", "-", "Test"));
        assertEquals("usage: Test --optWithValue <arg> -o <arg> --optVal/-p <arg> --noVal/-n [--optionWithValue/-f <arg>]" + System.lineSeparator(), duf.format(list, "--", "-", "Test"));
    }

}

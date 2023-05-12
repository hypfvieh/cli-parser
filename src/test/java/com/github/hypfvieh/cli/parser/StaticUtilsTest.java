package com.github.hypfvieh.cli.parser;

import org.junit.jupiter.api.Test;

class StaticUtilsTest extends AbstractBaseTest {

    @Test
    void testRequireOption() {
        assertThrows(NullPointerException.class, () -> StaticUtils.requireOption(null));

        CmdArgOption<?> opt = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .shortName('f')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> optShort = CmdArgOption.builder(String.class)
            .shortName('s')
            .name("")
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> optLong = CmdArgOption.builder(String.class)
            .name("long")
            .shortName(' ')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> optInvalid = CmdArgOption.builder(String.class)
            .name("")
            .shortName(' ')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .buildInvalid();

        assertEquals(opt, StaticUtils.requireOption(opt));
        assertEquals(optLong, StaticUtils.requireOption(optLong));
        assertEquals(optShort, StaticUtils.requireOption(optShort));

        assertThrows(CommandLineException.class, () -> StaticUtils.requireOption(optInvalid));

        CmdArgOption<?> invalidOpt = CmdArgOption.builder(String.class)
            .buildInvalid();

        assertThrows(CommandLineException.class, () -> StaticUtils.requireOption(invalidOpt));
    }

    @Test
    void testRequireUniqueOption() {
        CmdArgOption<?> opt1 = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .shortName('f')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> opt2 = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CmdArgOption<?> opt3 = CmdArgOption.builder(String.class)
            .shortName('f')
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CommandLine cl = new CommandLine().addOption(opt1);

        assertThrows(NullPointerException.class, () -> StaticUtils.requireUniqueOption(null, null));
        assertThrows(NullPointerException.class, () -> StaticUtils.requireUniqueOption(opt1, null));
        assertThrows(NullPointerException.class, () -> StaticUtils.requireUniqueOption(null, cl));

        assertThrows(CommandLineException.class, () -> StaticUtils.requireUniqueOption(opt2, cl));
        assertThrows(CommandLineException.class, () -> StaticUtils.requireUniqueOption(opt3, cl));
    }

    @Test
    void testCreateException() {
        RuntimeException rte = StaticUtils.createException("Hi", null);
        assertEquals(RuntimeException.class, rte.getClass());
        assertEquals("Hi", rte.getMessage());

        RuntimeException iae = StaticUtils.createException("Hello", CommandLineException.class);
        assertEquals(CommandLineException.class, iae.getClass());
        assertEquals("Hello", iae.getMessage());

        RuntimeException cle = StaticUtils.createException("Hallo", CommandLineException.class);
        assertEquals(CommandLineException.class, cle.getClass());
        assertEquals("Hallo", cle.getMessage());

        assertEquals(CommandLineException.class, StaticUtils.createException("fail", TestRte.class).getClass());
    }

    @Test
    void testRequireParsed() {
        CmdArgOption<?> opt = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .shortName('f')
            .optional()
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();

        CommandLine cl = new CommandLine().addOption(opt);

        assertThrows(NullPointerException.class, () -> StaticUtils.requireParsed(null));
        assertThrows(CommandLineException.class, () -> StaticUtils.requireParsed(cl));

        cl.parse("");
        assertEquals(cl, StaticUtils.requireParsed(cl));
    }

    @Test
    void testOptionNotDefined() {
        CmdArgOption<?> opt2 = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .required(true)
            .repeatable()
            .defaultValue("def")
            .description("descr")
            .build();
        assertEquals("Option not defined: null", StaticUtils.optionNotDefined(null, RuntimeException.class).getMessage());
        assertEquals("Option not defined: CmdArgOption[optionWithValue/-, dataType=java.lang.String, "
            + "required=true, repeatable=true, hasValue=true, default=def, descr=descr, possVals={}]", StaticUtils.optionNotDefined(opt2, RuntimeException.class).getMessage());
    }

    @Test
    void testTrimToNull() {
        assertEquals("hi", StaticUtils.trimToNull("hi"));
        assertNull(StaticUtils.trimToNull(" "));
        assertNull(StaticUtils.trimToNull(""));
        assertNull(StaticUtils.trimToNull(null));
    }

    @Test
    void testFormatOption() {
        CmdArgOption<?> opt1 = CmdArgOption.builder(String.class)
            .name("optionWithValue")
            .shortName('f')
            .required(true)
            .repeatable()
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

        CmdArgOption<?> optNullInvalid = CmdArgOption.builder(String.class)
            .buildInvalid();

        CmdArgOption<?> optEmptyInvalid = CmdArgOption.builder(String.class)
            .name("")
            .shortName(' ')
            .buildInvalid();

        CmdArgOption<?> optEmptyShortInvalid = CmdArgOption.builder(String.class)
            .name("foo")
            .shortName(' ')
            .buildInvalid();

        CmdArgOption<?> optEmptyLongInvalid = CmdArgOption.builder(String.class)
            .name("")
            .shortName('x')
            .buildInvalid();

        assertNull(StaticUtils.formatOption(null, null, null));

        assertEquals("-f/--optionWithValue", StaticUtils.formatOption(opt1, "--", "-"));
        assertEquals("--optWithValue", StaticUtils.formatOption(opt2, "--", "-"));
        assertEquals("-o", StaticUtils.formatOption(opt3, "--", "-"));
        assertEquals("?", StaticUtils.formatOption(optNullInvalid, "--", "-"));
        assertEquals("?", StaticUtils.formatOption(optEmptyInvalid, "--", "-"));
        assertEquals("--foo", StaticUtils.formatOption(optEmptyShortInvalid, "--", "-"));
        assertEquals("-x", StaticUtils.formatOption(optEmptyLongInvalid, "--", "-"));
    }

    /**
     * Sample RuntimeException which does not have a String argument constructor.
     */
    public static class TestRte extends RuntimeException {
        private static final long serialVersionUID = 1L;

        TestRte() {
        }
    }

}

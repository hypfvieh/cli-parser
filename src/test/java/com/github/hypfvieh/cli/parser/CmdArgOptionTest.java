package com.github.hypfvieh.cli.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CmdArgOptionTest extends AbstractBaseTest {

    @Test
    void buildInvalid() {
        // default value but no return type
        assertThrows(CommandLineException.class, () -> CmdArgOption.builder(null)
                .name("optionWithoutValue")
                .shortName('f')
                .required()
                .defaultValue("def"));

        // no name/shortname
        assertThrows(CommandLineException.class, () -> CmdArgOption.builder(String.class).build());
        // empty name
        assertThrows(CommandLineException.class, () -> CmdArgOption.builder(String.class).name("").build());
        // empty shortname
        assertThrows(CommandLineException.class, () -> CmdArgOption.builder(String.class).shortName(' ').build());
    }

    @Test
    void buildOptionWithValue() {
        CmdArgOption<?> opt = CmdArgOption.builder(String.class)
                .name("optionWithValue")
                .shortName('f')
                .required(true)
                .repeatable()
                .defaultValue("def")
                .description("descr")
                .build();

        assertEquals("optionWithValue", opt.getName());
        assertEquals(String.class, opt.getDataType());
        assertTrue(opt.isRequired());
        assertFalse(opt.isOptional());
        assertTrue(opt.hasValue());
        assertTrue(opt.isRepeatable());
        assertEquals("f", opt.getShortName());
        assertEquals("def", opt.getDefaultValue());
        assertEquals("descr", opt.getDescription());
        assertEquals("CmdArgOption[optionWithValue/f, dataType=java.lang.String, required=true, repeatable=true, hasValue=true, default=def, descr=descr]", opt.toString());
    }

    @Test
    void buildOptionWithoutValue() {
        CmdArgOption<?> opt = CmdArgOption.builder()
                .name("optionWithoutValue")
                .optional()
                .build();

        assertEquals("optionWithoutValue", opt.getName());
        assertNull(opt.getDataType());
        assertFalse(opt.isRequired());
        assertTrue(opt.isOptional());
        assertFalse(opt.hasValue());
        assertNull(opt.getDefaultValue());
        assertNull(opt.getDescription());
        assertEquals("CmdArgOption[optionWithoutValue/null, dataType=null, required=false, repeatable=false, hasValue=false, default=null, descr=null]", opt.toString());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void buildMissingNameFails(String _name) {
        assertEquals("Option requires a name or shortname", assertThrows(CommandLineException.class,
                () -> CmdArgOption.builder(String.class).name(_name).build()).getMessage());
    }

}

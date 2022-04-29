package com.github.hypfvieh.cli.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CmdArgOptionTest extends AbstractBaseTest {

    @Test
    void buildOptionWithValue() {
        CmdArgOption<?> opt = CmdArgOption.builder(String.class)
                .name("optionWithValue")
                .required(true)
                .defaultValue("def")
                .description("descr")
                .build();

        assertEquals("optionWithValue", opt.getName());
        assertEquals(String.class, opt.getDataType());
        assertTrue(opt.isRequired());
        assertFalse(opt.isOptional());
        assertTrue(opt.hasValue());
        assertEquals("def", opt.getDefaultValue());
        assertEquals("descr", opt.getDescription());
        assertEquals("CmdArgOption[optionWithValue, dataType=java.lang.String, required=true, hasValue=true, default=def, descr=descr]", opt.toString());
    }

    @Test
    void buildOptionWithoutValue() {
        CmdArgOption<?> opt = CmdArgOption.builder()
                .name("optionWithoutValue")
                .required(false)
                .build();

        assertEquals("optionWithoutValue", opt.getName());
        assertNull(opt.getDataType());
        assertFalse(opt.isRequired());
        assertTrue(opt.isOptional());
        assertFalse(opt.hasValue());
        assertNull(opt.getDefaultValue());
        assertNull(opt.getDescription());
        assertEquals("CmdArgOption[optionWithoutValue, dataType=null, required=false, hasValue=false, default=null, descr=null]", opt.toString());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void buildMissingNameFails(String _name) {
        assertEquals("Option requires a name", assertThrows(CommandLineException.class,
                () -> CmdArgOption.builder(String.class).name(_name).build()).getMessage());
    }

}

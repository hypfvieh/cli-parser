package com.github.hypfvieh.cli.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.hypfvieh.cli.parser.CommandLine.CommandLineException;
import com.github.hypfvieh.cli.parser.CommandLine.Option;

class OptionTest extends AbstractBaseTest {

    @Test
    void buildOptionWithValue() {
        Option<?> opt = Option.builder(String.class)
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
        assertEquals("Option[optionWithValue, dataType=java.lang.String, required=true, hasValue=true, default=def, descr=descr]", opt.toString());
    }

    @Test
    void buildOptionWithoutValue() {
        Option<?> opt = Option.builder()
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
        assertEquals("Option[optionWithoutValue, dataType=null, required=false, hasValue=false, default=null, descr=null]", opt.toString());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void buildMissingNameFails(String _name) {
        assertEquals("Option requires a name", assertThrows(CommandLineException.class,
                () -> Option.builder(String.class).name(_name).build()).getMessage());
    }

}

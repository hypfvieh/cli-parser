package com.github.hypfvieh.cli.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

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
        assertEquals("CmdArgOption[optionWithValue/f, dataType=java.lang.String, required=true, repeatable=true, hasValue=true, default=def, descr=descr, possVals={}]", opt.toString());
    }

    @Test
    void buildOptionWithAllowedValues() {
        Map<String, String> possVals = new LinkedHashMap<>();
        possVals.put("def", "Alphabet def");
        possVals.put("abc", "Alphabet abc");
        possVals.put("123", "Number 123");

        CmdArgOption<?> opt = CmdArgOption.builder(String.class)
            .name("optionWithAllowedValue")
            .shortName('f')
            .required(true)
            .defaultValue("def")
            .description("descr")
            .possibleValue(possVals)
            .build();

        assertEquals("optionWithAllowedValue", opt.getName());
        assertEquals(String.class, opt.getDataType());
        assertTrue(opt.isRequired());
        assertFalse(opt.isOptional());
        assertTrue(opt.hasValue());
        assertEquals("f", opt.getShortName());
        assertEquals("def", opt.getDefaultValue());
        assertEquals("descr", opt.getDescription());

        assertEquals("Alphabet def", opt.getPossibleValues().get("def"));
        assertEquals("Alphabet abc", opt.getPossibleValues().get("abc"));
        assertEquals("Number 123", opt.getPossibleValues().get("123"));

        assertNull(opt.getPossibleValues().get("blubb"));

        assertEquals("CmdArgOption[optionWithAllowedValue/f, dataType=java.lang.String, required=true, "
            + "repeatable=false, hasValue=true, default=def, descr=descr, "
            + "possVals={def=Alphabet def, abc=Alphabet abc, 123=Number 123}]",
            opt.toString());
    }

    @Test
    void buildOptionWithAllowedValuesInvalidDefault() {
        Map<String, String> possVals = new LinkedHashMap<>();
        possVals.put("def", "Alphabet def");
        possVals.put("abc", "Alphabet abc");
        possVals.put("123", "Number 123");

        CommandLineException ex = assertThrows(CommandLineException.class, () -> {
            CmdArgOption.builder(String.class)
                .name("optionWithAllowedValue")
                .shortName('f')
                .required(true)
                .defaultValue("foobar")
                .description("descr")
                .possibleValue(possVals)
                .build();
        });
        assertEquals("Option default value 'foobar' must be in possible value map", ex.getMessage());
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
        assertEquals("CmdArgOption[optionWithoutValue/-, dataType=null, required=false, repeatable=false, hasValue=false, default=null, descr=null, possVals={}]", opt.toString());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void buildMissingNameFails(String _name) {
        assertEquals("Option requires a name or shortname", assertThrows(CommandLineException.class,
            () -> CmdArgOption.builder(String.class).name(_name).build()).getMessage());
    }

}

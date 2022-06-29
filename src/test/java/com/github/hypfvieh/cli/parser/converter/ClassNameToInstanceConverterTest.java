package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CommandLineException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

class ClassNameToInstanceConverterTest extends AbstractBaseTest {

    @Test
    void testOk() {
        ClassNameToInstanceConverter<ClassNameToInstanceConverter<?>> converter = new ClassNameToInstanceConverter<>();
        assertInstanceOf(ClassNameToInstanceConverter.class, converter.convert(ClassNameToInstanceConverter.class.getName()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "string", "String.class", "java.lang.string"})
    void testConvertNullEmptyInvalid(String _str) {
        ClassNameToInstanceConverter<?> converter = new ClassNameToInstanceConverter<>();
        CommandLineException ex = assertThrows(CommandLineException.class, () -> converter.convert(_str));
        assertTrue(ex.getMessage().startsWith("Unable to create instance of class '"));
        assertNotNull(ex.getCause());
    }

    @Test
    void testConvertNoDefaultConstructor() {
        ClassNameToInstanceConverter<ClassNameToInstanceConverter<?>> converter = new ClassNameToInstanceConverter<>();
        CommandLineException ex = assertThrows(CommandLineException.class, () -> converter.convert(File.class.getName()));
        assertEquals("Unable to create instance of class '" + File.class.getName() + "'", ex.getMessage());
        assertInstanceOf(NoSuchMethodException.class, ex.getCause());
    }

}

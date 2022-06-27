package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CommandLineException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

class LocalTimeConverterTest extends AbstractBaseTest {

    @Test
    void testReadValid() {
        LocalTimeConverter converter = new LocalTimeConverter();

        assertEquals(LocalTime.of(13, 14, 15), converter.convert("13:14:15"));
        assertEquals(LocalTime.of(19, 20, 21), converter.convert("192021"));
    }

    @Test
    void testReadInValid() {
        LocalTimeConverter converter = new LocalTimeConverter();

        assertThrows(CommandLineException.class, () -> converter.convert("hi"));
    }

}

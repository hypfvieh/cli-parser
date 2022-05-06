package com.github.hypfvieh.cli.parser.converter;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CommandLineException;

class LocalDateTimeConverterTest extends AbstractBaseTest {

    @Test
    void testReadValid() {
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        
        assertEquals(LocalDateTime.of(2022, 1, 2, 11, 12, 13), converter.convert("2022-01-02 11:12:13"));
        assertEquals(LocalDateTime.of(1988, 11, 12, 14, 15, 16, 17), converter.convert("1988-11-12T14:15:16.000000017"));
    }

    @Test
    void testReadInValid() {
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        
        assertThrows(CommandLineException.class, () -> converter.convert("hi"));
    }
}

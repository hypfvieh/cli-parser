package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CommandLineException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class LocalDateConverterTest extends AbstractBaseTest {

    @Test
    void testReadValid() {
        LocalDateConverter converter = new LocalDateConverter();

        assertEquals(LocalDate.of(2022, 5, 5), converter.convert("2022-05-05"));
        assertEquals(LocalDate.of(1999, 8, 9), converter.convert("19990809"));
    }

    @Test
    void testReadInValid() {
        LocalDateConverter converter = new LocalDateConverter();

        assertThrows(CommandLineException.class, () -> converter.convert("hi"));
    }
}

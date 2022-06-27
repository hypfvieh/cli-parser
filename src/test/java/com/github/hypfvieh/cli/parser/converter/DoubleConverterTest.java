package com.github.hypfvieh.cli.parser.converter;

import com.github.hypfvieh.cli.parser.AbstractBaseTest;
import com.github.hypfvieh.cli.parser.CommandLineException;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.Locale;

class DoubleConverterTest extends AbstractBaseTest {

    @Test
    void testReadValid() {
        DoubleConverter converter = new DoubleConverter();
        converter.addPattern(DecimalFormat.getInstance(Locale.GERMAN));

        assertEquals(17.4, converter.convert("17.4"));
        assertEquals(8.12, converter.convert("8,12"));
    }

    @Test
    void testReadInValid() {
        DoubleConverter converter = new DoubleConverter();

        assertThrows(CommandLineException.class, () -> converter.convert("hi"));
    }

}

package com.github.hypfvieh.cli.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CommandLineExceptionTest {

    @Test
    void testConstructors() {
        CommandLineException cle = new CommandLineException("Hi");
        assertEquals("Hi", cle.getMessage());
        assertNull(cle.getCause());

        CommandLineException cle2 = new CommandLineException("Fail", cle);
        assertEquals("Fail", cle2.getMessage());
        assertNotNull(cle2.getCause());
        assertEquals(cle, cle2.getCause());
    }

}

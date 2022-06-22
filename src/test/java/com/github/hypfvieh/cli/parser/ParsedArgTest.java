package com.github.hypfvieh.cli.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParsedArgTest {

    @Test
    void testToString() {
        ParsedArg parsedArg = new ParsedArg(false, true, null);
        assertEquals("ParsedArg [lookingLikeOption=false, multiArg=true, cmdArgOpt=null, value=null]", parsedArg.toString());
    }

}

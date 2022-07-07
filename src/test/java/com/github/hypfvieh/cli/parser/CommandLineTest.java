package com.github.hypfvieh.cli.parser;

import com.github.hypfvieh.cli.parser.formatter.IUsageFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

class CommandLineTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    public void addOptionWithoutOrInvalidNameFail(String _argName) {
        assertEquals("Option requires a name or shortname",
                assertThrows(CommandLineException.class,
                        () -> new CommandLine().addOption(CmdArgOption.builder(String.class)
                                .name(_argName)
                                .optional()
                                .build())).getMessage());
    }

    @Test
    public void setExceptionTypeOk() {
        CommandLine cl = new CommandLine();

        assertEquals(CommandLineException.class, cl.getExceptionType());
        assertEquals("Command-line not parsed",
                assertThrows(Exception.class, cl::getKnownArgs).getMessage());

        cl.withExceptionType(IllegalArgumentException.class);
        assertEquals(IllegalArgumentException.class, cl.getExceptionType());
        assertEquals("Command-line not parsed",
                assertThrows(IllegalArgumentException.class, cl::getKnownArgs).getMessage());
    }

    @Test
    public void setExceptionTypeFail() {
        CommandLine cl = new CommandLine();

        assertEquals(CommandLineException.class, cl.getExceptionType());
        assertEquals("Exception type requires a single-argument constructor of type String",
                assertThrows(Exception.class, () -> cl.withExceptionType(NoArgException.class)).getMessage());
        assertEquals(CommandLineException.class, cl.getExceptionType());
    }

    private static final class NoArgException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private NoArgException() {
        }
    }

    @Test
    public void addSameOptionMoreThanOnce() {
        String optName = "arg1";
        CommandLine cl = new CommandLine()
                .addOption(CmdArgOption.builder(String.class)
                        .name(optName)
                        .optional()
                        .defaultValue("default1")
                        .build());

        assertFalse(cl.getOption(optName).isRequired());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cl.addOption(CmdArgOption.builder(String.class)
                        .name(optName)
                        .required()
                        .defaultValue("default2")
                        .build()));

        assertEquals("Command-line option '--arg1' already defined", ex.getMessage());
    }

    @Test
    public void testGetUndefinedOptionFromCommandlineFail() {
        CommandLine cl = new CommandLine().parse((String) null);
        assertEquals("Option not defined: arg1",
                assertThrows(Exception.class, () -> cl.getArg("arg1")).getMessage());
        assertFalse(cl.hasOption("arg2"));
    }

    @Test
    public void testHasOption() {
        CmdArgOption<String> used = CmdArgOption.builder(String.class)
                .name("arg")
                .optional()
                .defaultValue("default1")
                .build();

        CmdArgOption<String> usedMulti = CmdArgOption.builder(String.class)
                .name("marg")
                .optional()
                .repeatable()
                .defaultValue("default1")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(used)
                .addOptions((CmdArgOption<?>[]) null) // should be accepted and ignored
                .addOption(usedMulti);

        CmdArgOption<String> notUsed = CmdArgOption.builder(String.class)
                .name("xarg")
                .optional()
                .defaultValue("default1")
                .build();

        assertFalse(cl.hasOption(notUsed));
        assertTrue(cl.hasOption(used));
        assertTrue(cl.hasOption(usedMulti));
    }

    @Test
    public void testGetOptionFromUnparsedCommandlineFail() {
        CmdArgOption<Long> optAlong = CmdArgOption.builder(Long.class)
                .name("along")
                .required()
                .build();
        CommandLine cl = new CommandLine().addOption(optAlong);
        assertTrue(cl.hasOption(optAlong));
        assertTrue(cl.hasOption("along"));
        assertEquals("Command-line not parsed",
                assertThrows(Exception.class, () -> cl.getArg("along")).getMessage());
    }

    @Test
    public void testGetOptionNoValue() {
        CmdArgOption<Void> optNoVal = CmdArgOption.builder()
                .name("optNoVal")
                .required()
                .description("required no value option")
                .build();

        CommandLine cl = new CommandLine().addOption(optNoVal);
        assertTrue(cl.hasOption(optNoVal));
        assertTrue(cl.hasOption("optNoVal"));

        assertDoesNotThrow(() -> cl.parse(new String[] {"--optNoVal"}));
    }

    @Test
    public void testGetOptionNoValueWithValue() {
        CmdArgOption<Void> optNoVal = CmdArgOption.builder()
                .name("optNoVal")
                .required()
                .description("required no value option")
                .build();

        CommandLine cl = new CommandLine().addOption(optNoVal);
        assertTrue(cl.hasOption(optNoVal));
        assertTrue(cl.hasOption("optNoVal"));

        assertEquals("Parsing of command-line failed: argument '--" + optNoVal.getName() + "' cannot have a value",
                assertThrows(Exception.class, () -> cl.parse("--optNoVal doesHaveValue")).getMessage());
    }

    @Test
    public void parseEmpty() {
        CommandLine cl = new CommandLine();
        cl.parse((String) null);
        cl.parse((String[]) null);
        cl.parse(new String[0]);
        cl.parse(new String[] {null});
        cl.parse(new String[] {null, "", "\t"});

        assertTrue(cl.getKnownArgs().isEmpty());
    }

    @Test
    public void parseShortAndLongOptions() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .name("optional-int")
                .optional()
                .description("optional int")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false)
                .parse("-o 1");

        assertFalse(cl.isFailOnUnknownToken());
        Integer shortArgVal = cl.getArg(optInt);
        cl.parse("--optional-int 2");
        Integer longArgVal = cl.getArg(optInt);

        assertEquals(1, shortArgVal);
        assertEquals(2, longArgVal);
    }

    @Test
    public void parseShortAndLongOptionsByName() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .name("opt-int")
                .optional()
                .description("optional int")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false);

        assertFalse(cl.isFailOnUnknownToken());

        assertEquals(1, cl.parse("-o 1").getArg('o'));
        assertEquals(2, cl.parse("--opt-int 2").getArg("opt-int"));
    }

    @Test
    public void parseSingleLongOptionsEqualSign() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .name("opt-int")
                .optional()
                .description("optional int")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false);

        assertFalse(cl.isFailOnUnknownToken());

        assertEquals(2, cl.parse("--opt-int=2").getArg(optInt));
    }

    @Test
    public void parseSingleShortOptionsEqualSign() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .name("opt-int")
                .optional()
                .description("optional int")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false);

        assertFalse(cl.isFailOnUnknownToken());

        assertEquals(2, cl.parse("--o=2").getArg(optInt));
    }

    @Test
    public void parseMultiLongOptionsEqualSign() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .name("opt-int")
                .optional()
                .description("optional int")
                .build();

        CmdArgOption<String> optVal = CmdArgOption.builder(String.class)
                .shortName('s')
                .name("opt-string")
                .optional()
                .description("optional string")
                .build();

        CommandLine cl = new CommandLine()
                .addOptions(optInt, optVal)
                .withFailOnUnknownToken(false);

        assertFalse(cl.isFailOnUnknownToken());

        CommandLine parse = cl.parse("--opt-int=2 --opt-string=foo");
        assertEquals(2, parse.getArg(optInt));
        assertEquals("foo", parse.getArg(optVal));
    }

    @Test
    public void parseMultiShortOptionsEqualSign() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .name("opt-int")
                .optional()
                .description("optional int")
                .build();

        CmdArgOption<String> optVal = CmdArgOption.builder(String.class)
                .shortName('s')
                .name("opt-string")
                .optional()
                .description("optional string")
                .build();

        CommandLine cl = new CommandLine()
                .addOptions(optInt, optVal)
                .withFailOnUnknownToken(false);

        assertFalse(cl.isFailOnUnknownToken());

        CommandLine parse = cl.parse("-o=2 -s=foo");
        assertEquals(2, parse.getArg(optInt));
        assertEquals("foo", parse.getArg(optVal));
    }

    @Test
    public void parseCombinedShortOptionsWithEqualSign() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .optional()
                .description("optional int")
                .build();
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName('a')
                .optional()
                .description("all flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .addOption(optAll)
                .withFailOnUnknownToken(false)
                .parse("-ao=10");

        assertFalse(cl.isFailOnUnknownToken());

        assertTrue(cl.hasArg(optAll));
        assertEquals(10, cl.getArg(optInt));
    }

    @Test
    public void parseShortOptions() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .optional()
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false)
                .parse("-o 1 2 3");

        assertFalse(cl.isFailOnUnknownToken());
        assertCollection(cl.getUnknownTokens(), "2", "3");

        assertEquals("Parsing of command-line failed: unknown tokens: 2, 3",
                assertThrows(Exception.class, () -> cl.withFailOnUnknownToken(true)
                        .parse("-o 1 2 3")).getMessage());
    }

    @Test
    public void parseCombinedShortOptions() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .optional()
                .description("optional int")
                .build();
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName('a')
                .optional()
                .description("all flag")
                .build();

        CmdArgOption<?> optRepeat = CmdArgOption.builder()
                .shortName('r')
                .optional()
                .repeatable()
                .description("repeatable flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .addOption(optAll)
                .addOption(optRepeat)
                .withFailOnUnknownToken(false)
                .parse("-rrr -ao 1 2 3");

        assertFalse(cl.isFailOnUnknownToken());
        assertCollection(cl.getUnknownTokens(), "2", "3");

        assertTrue(cl.hasArg(optAll));
        assertTrue(cl.hasArg(optInt));
    }

    @Test
    public void parseRepeatableOptions() {
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName('a')
                .optional()
                .description("all flag")
                .build();

        CmdArgOption<?> optRepeat = CmdArgOption.builder()
                .shortName('r')
                .optional()
                .repeatable()
                .description("repeatable flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .addOption(optRepeat)
                .withFailOnUnknownToken(false)
                .parse("-rrr -a");

        assertEquals(3, cl.getArgCount(optRepeat));
        assertTrue(cl.hasArg(optAll));
    }

    @Test
    public void parseMultiRepeatableOptions() {
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName('a')
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CmdArgOption<?> optRepeat = CmdArgOption.builder()
                .shortName('s')
                .optional()
                .repeatable()
                .description("second flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .addOption(optRepeat)
                .withFailOnUnknownToken(false)
                .parse("-aaa -sss -assa");

        assertEquals(5, cl.getArgCount(optRepeat));
        assertEquals(5, cl.getArgCount(optAll));
    }

    @Test
    public void parseMultiRepeatableOptionsWithValueCombinedShort() {
        CmdArgOption<?> optAll = CmdArgOption.builder(String.class)
                .shortName('a')
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CmdArgOption<?> optRepeat = CmdArgOption.builder(String.class)
                .shortName('s')
                .optional()
                .repeatable()
                .description("second flag")
                .build();

        assertThrows(CommandLineException.class, () -> new CommandLine()
                .addOption(optAll)
                .addOption(optRepeat)
                .withFailOnUnknownToken(false)
                .parse("-as test -sa case"));

    }

    @Test
    public void parseMultiRepeatableLongOptionsWithValue() {
        CmdArgOption<?> optAll = CmdArgOption.builder(String.class)
                .name("first")
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CmdArgOption<?> optRepeat = CmdArgOption.builder(String.class)
                .name("second")
                .optional()
                .repeatable()
                .description("second flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .addOption(optRepeat)
                .withFailOnUnknownToken(false)
                .parse("--first hans --first wurst --second harry --second hirsch");

        assertEquals(2, cl.getArgCount(optRepeat));
        assertEquals(2, cl.getArgCount(optAll));

        assertEquals("hans", cl.getArgs(optAll).get(0));
        assertEquals("wurst", cl.getArgs(optAll).get(1));

        assertEquals("harry", cl.getArgs(optRepeat).get(0));
        assertEquals("hirsch", cl.getArgs(optRepeat).get(1));
    }

    @Test
    public void parseOneUnknownArg() {
        CommandLine cl = new CommandLine()
                .withFailOnUnknownArg(false)
                .parse("--first");

        assertEquals("--first", cl.getUnknownArgs().keySet().iterator().next());
    }

    @Test
    public void parseUnknownFirstToken() {
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .name("second")
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .withFailOnUnknownArg(false)
                .withFailOnUnknownToken(false)
                .parse("first --second");

        assertEquals("first", cl.getUnknownTokens().get(0));
        assertTrue(cl.hasArg(optAll));
    }

    @Test
    public void parseUnknownShort() {
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName('f')
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .withFailOnUnknownArg(false)
                .withFailOnUnknownToken(false)
                .parse("-af");

        assertEquals("a", cl.getUnknownArgs().keySet().iterator().next());
        assertTrue(cl.hasArg(optAll));
    }

    @Test
    public void parseUnknownShortOpt() {
        CommandLine cl = new CommandLine()
                .withFailOnUnknownArg(false)
                .withFailOnUnknownToken(false)
                .parse("-a");

        assertEquals("-a", cl.getArgBundle().getUnknownArgs().keySet().iterator().next());

    }

    @Test
    public void parseUnknownToken2() {
        CommandLine cl = new CommandLine()
                .withFailOnUnknownArg(false)
                .withFailOnUnknownToken(false)
                .parse("a");

        assertEquals("a", cl.getArgBundle().getUnknownTokens().get(0));
    }

    @Test
    public void parseUnknownArgDelim() {
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName('f')
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .withLongOptPrefix(null) // should change nothing
                .withShortOptPrefix(null) // should change nothing
                .withFailOnUnknownArg(false)
                .withFailOnUnknownToken(false)
                .parse("---f");

        assertEquals("---f", cl.getUnknownTokens().get(0));
    }

    @Test
    public void parseUnknownShortArg() {
        CommandLine cl = new CommandLine()
                .withFailOnUnknownArg(false)
                .withFailOnUnknownToken(false)
                .parse("-f");

        assertEquals("-f", cl.getUnknownArgs().keySet().iterator().next());
    }

    @Test
    public void parseMultiRepeatableShortOptionsWithValue() {
        CmdArgOption<?> optAll = CmdArgOption.builder(String.class)
                .shortName('a')
                .optional()
                .repeatable()
                .description("first flag")
                .build();

        CmdArgOption<?> optRepeat = CmdArgOption.builder(String.class)
                .shortName('s')
                .optional()
                .repeatable()
                .description("second flag")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optAll)
                .addOption(optRepeat)
                .withFailOnUnknownToken(false)
                .parse("-a hans -a wurst -s harry -s hirsch");

        assertEquals(2, cl.getArgCount(optRepeat));
        assertEquals(2, cl.getArgCount(optAll));

        assertEquals("hans", cl.getArgs(optAll).get(0));
        assertEquals("wurst", cl.getArgs(optAll).get(1));

        assertEquals("harry", cl.getArgs(optRepeat).get(0));
        assertEquals("hirsch", cl.getArgs(optRepeat).get(1));
    }

    @Test
    public void parseMultiOptions() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName('o')
                .optional()
                .repeatable(true)
                .description("optional int")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false)
                .parse("-o 1 -o 2 -o 3");

        assertTrue(cl.hasArg(optInt));

        assertTrue(cl.getUnknownTokens().isEmpty());

        assertCollection(cl.getArgs(optInt), 1, 2, 3);
    }

    @Test
    public void parseMultiVoidOptions() {
        CmdArgOption<?> optInt = CmdArgOption.builder()
                .shortName('o')
                .optional()
                .repeatable(true)
                .description("optional")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false)
                .parse("-ooo");

        assertTrue(cl.hasArg(optInt));

        assertTrue(cl.getUnknownTokens().isEmpty());
        assertNull(cl.getArg(optInt));
        assertEquals(3, cl.getArgCount(optInt));
    }

    @Test
    public void testArgCount() {
        CmdArgOption<?> optInt = CmdArgOption.builder()
                .shortName('o')
                .optional()
                .repeatable()
                .description("optional")
                .build();

        CmdArgOption<?> optInt2 = CmdArgOption.builder()
                .shortName('i')
                .optional()
                .description("optional")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .addOption(optInt2)
                .withFailOnUnknownToken(false);

        assertEquals(0, cl.parse("").getArgCount(optInt));
        assertEquals(1, cl.parse("-o").getArgCount(optInt));
        assertEquals(1, cl.parse("-i").getArgCount(optInt2));
        assertEquals(2, cl.parse("-oo").getArgCount(optInt));
        assertEquals(10, cl.parse("-oooooooooo").getArgCount(optInt));
    }

    @Test
    public void testHasArg() {
        CmdArgOption<?> optInt = CmdArgOption.builder()
                .shortName('o')
                .optional()
                .repeatable()
                .description("optional")
                .build();

        CmdArgOption<?> optInt2 = CmdArgOption.builder()
                .name("long")
                .optional()
                .description("optional")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .addOption(optInt2)
                .withFailOnUnknownToken(false);


        assertTrue(cl.parse("--long").hasArg(optInt2), "Cmd Long Option set");
        assertTrue(cl.parse("-o").hasArg(optInt), "Cmd Short Option set");

        assertFalse(cl.parse("").hasArg("long"), "Long Option not set");
        assertTrue(cl.parse("--long").hasArg("long"), "Long Option set");

        assertFalse(cl.parse("").hasArg('o'), "Short Option not set");
        assertTrue(cl.parse("-o").hasArg('o'), "Short Option set");
    }

    @Test
    public void parseDataTypes() {
        CmdArgOption<Boolean> optBoolSimple = CmdArgOption.builder(boolean.class)
                .name("optBoolSimple")
                .required()
                .build();
        CmdArgOption<Boolean> optBoolWrapper = CmdArgOption.builder(Boolean.class)
                .name("optBoolWrapper")
                .required()
                .build();

        CmdArgOption<Byte> optByte = CmdArgOption.builder(Byte.class).name("optByte").required().build();
        CmdArgOption<Short> optShort = CmdArgOption.builder(Short.class).name("optShort").required().build();

        CmdArgOption<Integer> optIntReq = CmdArgOption.builder(int.class).name("optIntReq").required().build();
        CmdArgOption<Integer> optIntOpt = CmdArgOption.builder(int.class).name("optIntOpt").optional().defaultValue(-1).build();

        CmdArgOption<Long> optLong = CmdArgOption.builder(Long.class).name("optLong").required().build();
        CmdArgOption<Float> optFloat = CmdArgOption.builder(Float.class).name("optFloat").required().build();
        CmdArgOption<Double> optDouble = CmdArgOption.builder(Double.class).name("optDouble").required().build();
        CmdArgOption<String> optString = CmdArgOption.builder(String.class).name("optString").required().build();

        CmdArgOption<LocalDate> optLocalDate = CmdArgOption.builder(LocalDate.class)
                .name("optLocalDate")
                .required()
                .build();

        CmdArgOption<LocalDateTime> optLocalDateTime = CmdArgOption.builder(LocalDateTime.class)
                .name("optLocalDateTime")
                .required()
                .build();

        CmdArgOption<LocalTime> optLocalTime = CmdArgOption.builder(LocalTime.class)
                .name("optLocalTime")
                .required()
                .build();

        CommandLine cl = new CommandLine()
                .addOptions(optBoolSimple, optBoolWrapper, optByte, optShort)
                .addOptions(optIntReq, optIntOpt)
                .addOptions(optLong, optFloat, optDouble)
                .addOptions(optString, optLocalDate, optLocalDateTime, optLocalTime);

        cl.getOptions().values().stream().map(cl::hasOption).forEach(CommandLineTest::assertTrue);

        cl.getOptions().values().stream().map(CmdArgOption::getName).map(cl::hasOption).forEach(CommandLineTest::assertTrue);

        cl.parse(String.join(" " ,
                 "--optBoolSimple true"
               , "--optBoolWrapper FALSE"
               , "--optByte 127"
               , "--optShort 32767"
               , "--optIntReq 99"
               , "--optLong 123456789"
               , "--optFloat 654.321"
               , "--optDouble 987654.321"
               , "--optString a_string"
               , "--optLocalDate 2025-04-07"
               , "--optLocalDateTime 2028-09-22T19:42:58"
               , "--optLocalTime 21:30:15.789"
               ));

        cl.getOptions().values().stream().filter(CmdArgOption::isRequired).map(cl::hasArg).forEach(CommandLineTest::assertTrue);

        assertEquals(LocalDate.of(2025, 4, 7), cl.getArg(optLocalDate));
        assertEquals(LocalDateTime.of(2028, 9, 22, 19, 42, 58), cl.getArg(optLocalDateTime));
        assertEquals(LocalTime.of(21, 30, 15, 789000000), cl.getArg(optLocalTime));
        assertEquals(99, cl.getArg(optIntReq));
        assertEquals(-1, cl.getArg(optIntOpt));
    }

    @Test
    public void parseUnknownArg() {
        CommandLine cl = new CommandLine()
                .withFailOnUnknownArg(false)
                .parse("--dunno this");

        assertFalse(cl.isFailOnUnknownArg());
        assertMap(cl.getUnknownArgs(), "--dunno");
        assertEquals("this", cl.getUnknownArgs().get("--dunno"));

        assertEquals("Parsing of command-line failed: unknown arguments: --dunno=this",
                assertThrows(Exception.class, () -> cl.withFailOnUnknownArg(true)
                        .parse("--dunno this")).getMessage());
    }

    @Test
    public void parseUnknownToken() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .name("optInt")
                .optional()
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .withFailOnUnknownToken(false)
                .parse("--optInt 1 2 3");
        assertFalse(cl.isFailOnUnknownToken());
        assertCollection(cl.getUnknownTokens(), "2", "3");

        assertEquals("Parsing of command-line failed: unknown tokens: 2, 3",
                assertThrows(Exception.class, () -> cl.withFailOnUnknownToken(true)
                        .parse("--optInt 1 2 3")).getMessage());
    }

    @Test
    public void parseDupArg() {
        CmdArgOption<Void> optDup = CmdArgOption.builder()
                .name("optdup")
                .optional()
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optDup)
                .withFailOnDupArg(false)
                .parse("--optdup --optdup");

        assertFalse(cl.isFailOnDupArg());

        assertTrue(cl.getDupArgs().containsKey(optDup));

        assertEquals("Parsing of command-line failed: duplicate arguments: --optdup",
                assertThrows(Exception.class, () -> cl.withFailOnDupArg(true)
                        .parse("--optdup --optdup")).getMessage());
    }

    @Test
    public void parseArgTyped() {
        CmdArgOption<Integer> optDup = CmdArgOption.builder(Integer.class)
                .name("optdup")
                .optional()
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optDup)
                .withFailOnDupArg(false)
                .parse("--optdup 1");

        assertEquals(1, cl.getArg("optdup", Integer.class));

        CommandLineException ex = assertThrows(CommandLineException.class, () -> {
            cl.getArg("optdup", Long.class);
        });

        assertEquals("Invalid type conversation, expected: java.lang.Integer - found: java.lang.Long", ex.getMessage());
    }

    @Test
    public void parseArgListTyped() {
        CmdArgOption<Integer> optDup = CmdArgOption.builder(Integer.class)
                .name("optdup")
                .optional()
                .repeatable()
                .description("optional int")
                .build();

        CommandLine cl = new CommandLine()
                .addOption(optDup)
                .withFailOnDupArg(false)
                .parse("--optdup 1 --optdup 2 --optdup 3");

        List<Integer> args = cl.getArgs("optdup", Integer.class);
        assertEquals(1, args.get(0));
        assertEquals(2, args.get(1));
        assertEquals(3, args.get(2));

        CommandLineException ex = assertThrows(CommandLineException.class, () -> {
            cl.getArgs("optdup", Long.class);
        });

        assertEquals("Invalid type conversation, expected: java.lang.Integer - found: java.lang.Long", ex.getMessage());
    }

    @Test
    public void parseArgTypedDefault() {
        CmdArgOption<Integer> optDup = CmdArgOption.builder(Integer.class)
                .name("optdup")
                .optional()
                .defaultValue(10)
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optDup)
                .withFailOnDupArg(false)
                .withFailOnUnknownArg(false)
                .parse("--bla");

        assertEquals(5, cl.getArg("optdup", Integer.class, 5));
    }

    @Test
    public void parseRequiredOptionMissingFail() {
        CommandLine cl = new CommandLine().addOption(CmdArgOption.builder(String.class)
                .name("requiredString")
                .required()
                .build());
        assertEquals("Parsing of command-line failed: required options missing: requiredString",
                assertThrows(Exception.class, () -> cl.parse("")).getMessage());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"true", " true ", "TRUE", "1", "yes"})
    public void parseBooleanArgTrue(String _boolArg) {
        CmdArgOption<Boolean> optBool = CmdArgOption.builder(boolean.class)
                .name("optBool")
                .build();
        CommandLine cl = new CommandLine().addOption(optBool);
        assertTrue(cl.parse(new String[] {"--" + optBool.getName(), _boolArg}).getArg(optBool));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "false", "FALSE", "0", "no"})
    public void parseBooleanArgFalse(String _boolArg) {
        CmdArgOption<Boolean> optBool = CmdArgOption.builder(boolean.class)
                .name("optBool")
                .defaultValue(false)
                .build();
        CommandLine cl = new CommandLine().addOption(optBool);
        assertFalse(cl.parse(new String[] {"--" + optBool.getName(), _boolArg}).getArg(optBool));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    public void parseDateArgNullEmptyBlank(String _dateArg) {
        CmdArgOption<LocalDate> optDate = CmdArgOption.builder(LocalDate.class)
                .name("optDate")
                .required()
                .build();
        CommandLine cl = new CommandLine().addOption(optDate);
        assertEquals("Parsing of command-line failed: argument '--optDate' requires a value",
                assertThrows(CommandLineException.class, () -> cl.parse(new String[] {"--" + optDate.getName(), _dateArg}).getArg(optDate)).getMessage());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"xy", "9999-88-77", "66665544"})
    public void parseDateArgInvalid(String _dateArg) {
        CmdArgOption<LocalDate> optDate = CmdArgOption.builder(LocalDate.class)
                .name("optDate")
                .required()
                .description("descr")
                .build();
        CommandLine cl = new CommandLine().addOption(optDate);
        CommandLineException ex = assertThrows(CommandLineException.class, () -> cl.parse(new String[] {"--optDate", _dateArg}));
        assertEquals("Parsing of command-line failed: argument '--optDate' has invalid value (" + _dateArg + ")", ex.getMessage());
    }

    @Test
    public void parseDateArgDefault() {
        LocalDate defLd = LocalDate.now();
        CmdArgOption<LocalDate> optLd = CmdArgOption.builder(LocalDate.class)
                .name("optDate")
                .defaultValue(defLd)
                .build();

        LocalDateTime defLdt = LocalDateTime.now();
        CmdArgOption<LocalDateTime> optLdt = CmdArgOption.builder(LocalDateTime.class)
                .name("optDateTime")
                .defaultValue(defLdt)
                .build();

        CommandLine cl = new CommandLine().addOptions(optLd, optLdt).parse("--optDate --optDateTime");
        assertEquals(defLd, cl.getArg(optLd));
        assertEquals(defLdt, cl.getArg(optLdt));
    }

    @Test
    public void testGetUsageNoArgs() {
        assertPatternMatches(new CommandLine().getUsage(null), "^usage: [^ ]+\n");
    }

    @Test
    public void testGetUsageWithArgs() {
        CommandLine cl = new CommandLine()
                .addOption(CmdArgOption.builder(String.class)
                        .name("req1")
                        .required()
                        .defaultValue("default")
                        .build())
                .addOption(CmdArgOption.builder()
                        .name("opt1")
                        .optional()
                        .build());
        assertEquals("usage: myApp --req1 <arg> [--opt1]" + System.lineSeparator(), cl.getUsage("myApp"));
    }

    @Test
    public void printUsage() throws IOException {

        String help;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); var ps = new PrintStream(baos)) {
            new CommandLine().printUsage("myApp", new PrintStream(baos));
            help = baos.toString();
        }
        assertTrue(help.startsWith("usage: myApp" + System.lineSeparator()));
    }

    @Test
    public void printUsage2() throws IOException {

        String help;
        PrintStream oldOut = System.out;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); var ps = new PrintStream(baos)) {
            System.setOut(ps);
            new CommandLine().printUsage();
            help = baos.toString();
        } finally {
            System.setOut(oldOut);
        }
        String mainClassName = IUsageFormatter.getMainClassName();
        System.out.println(help);
        assertTrue(help.startsWith("usage: " + mainClassName + System.lineSeparator()));
    }

    @Test
    public void printUsageCustomFormatter() throws IOException {
        String help = new CommandLine().withUsageFormatter((options, l, s, mainClassName) -> "Test the Usage-Formatter: -->" + mainClassName + "<--: ").getUsage("App");
        assertTrue(help.startsWith("Test the Usage-Formatter: -->App<--: "));
    }
}

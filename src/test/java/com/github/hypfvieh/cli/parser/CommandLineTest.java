package com.github.hypfvieh.cli.parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.hypfvieh.cli.parser.CommandLine.CommandLineException;
import com.github.hypfvieh.cli.parser.CommandLine.Option;

class CommandLineTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    public void addOptionWithoutOrInvalidNameFail(String _argName) {
        assertEquals("Option requires a name",
                assertThrows(CommandLineException.class,
                        () -> new CommandLine().addOption(Option.builder(String.class)
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

        cl.setExceptionType(IllegalArgumentException.class);
        assertEquals(IllegalArgumentException.class, cl.getExceptionType());
        assertEquals("Command-line not parsed",
                assertThrows(IllegalArgumentException.class, cl::getKnownArgs).getMessage());
    }

    @Test
    public void setExceptionTypeFail() {
        CommandLine cl = new CommandLine();

        assertEquals(CommandLineException.class, cl.getExceptionType());
        assertEquals("Exception type requires a single-argument constructor of type String",
                assertThrows(Exception.class, () -> cl.setExceptionType(NoArgException.class)).getMessage());
        assertEquals(CommandLineException.class, cl.getExceptionType());
    }

    private static final class NoArgException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private NoArgException() {
        }
    }

    @Test
    public void addSameOptionMoreThanOnceOk() {
        String optName = "arg1";
        CommandLine cl = new CommandLine()
                .addOption(Option.builder(String.class)
                        .name(optName)
                        .optional()
                        .defaultValue("default1")
                        .build());

        assertFalse(cl.getOption(optName).isRequired());

        cl.addOption(Option.builder(String.class)
                .name(optName)
                .required()
                .defaultValue("default2")
                .build());

        assertTrue(cl.getOption(optName).isRequired());
        assertEquals("default2", cl.getOption(optName).getDefaultValue());
    }

    @Test
    public void getUndefinedOptionFromCommandlineFail() {
        CommandLine cl = new CommandLine().parse((String) null);
        assertEquals("Option not defined: arg1",
                assertThrows(Exception.class, () -> cl.getArg("arg1")).getMessage());
        assertFalse(cl.hasOption("arg2"));
    }

    @Test
    public void getOptionFromUnparsedCommandlineFail() {
        Option<Long> optAlong = Option.builder(Long.class)
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
    public void getOptionNoValue() {
        Option<Void> optNoVal = Option.builder()
                .name("optNoVal")
                .required()
                .description("required no value option")
                .build();

        CommandLine cl = new CommandLine().addOption(optNoVal);
        assertTrue(cl.hasOption(optNoVal));
        assertTrue(cl.hasOption("optNoVal"));

        assertDoesNotThrow(() -> cl.parse(new String[] {"--optNoVal"}));
        assertEquals("Parsing of command-line failed: argument '" + optNoVal.getName() + "' cannot have a value",
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

    /**
     *
     */
    @Test
    public void parseDataTypes() {
        Option<Boolean> optBoolSimple = Option.builder(boolean.class)
                .name("optBoolSimple")
                .required()
                .build();
        Option<Boolean> optBoolWrapper = Option.builder(Boolean.class)
                .name("optBoolWrapper")
                .required()
                .build();

        Option<Byte> optByte = Option.builder(Byte.class).name("optByte").required().build();
        Option<Short> optShort = Option.builder(Short.class).name("optShort").required().build();

        Option<Integer> optIntReq = Option.builder(int.class).name("optIntReq").required().build();
        Option<Integer> optIntOpt = Option.builder(int.class).name("optIntOpt").optional().defaultValue(-1).build();

        Option<Long> optLong = Option.builder(Long.class).name("optLong").required().build();
        Option<Float> optFloat = Option.builder(Float.class).name("optFloat").required().build();
        Option<Double> optDouble = Option.builder(Double.class).name("optDouble").required().build();
        Option<String> optString = Option.builder(String.class).name("optString").required().build();

        Option<LocalDate> optLocalDate = Option.builder(LocalDate.class)
                .name("optLocalDate")
                .required()
                .build();

        CommandLine cl = new CommandLine()
               .addOptions(optBoolSimple, optBoolWrapper, optByte, optShort)
               .addOptions(optIntReq, optIntOpt)
               .addOptions(optLong, optFloat, optDouble)
               .addOptions(optString, optLocalDate);

        cl.getOptions().values().stream().map(cl::hasOption).forEach(CommandLineTest::assertTrue);

        cl.getOptions().values().stream().map(Option::getName).map(cl::hasOption).forEach(CommandLineTest::assertTrue);

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
               , "--optLocalDate 2025-04-07"));

        cl.streamOptions(Option::isRequired).map(cl::hasArg).forEach(CommandLineTest::assertTrue);

        assertEquals(LocalDate.of(2025, 4, 7), cl.getArg(optLocalDate));
        assertEquals(99, cl.getArg(optIntReq));
        assertEquals(-1, cl.getArg(optIntOpt));
    }

    @Test
    public void parseUnknownArg() {
        CommandLine cl = new CommandLine()
                .setFailOnUnknownArg(false)
                .parse("--dunno this");
        assertFalse(cl.isFailOnUnknownArg());
        assertMap(cl.getUnknownArgs(), "dunno");
        assertEquals("this", cl.getUnknownArgs().get("dunno"));

        assertEquals("Parsing of command-line failed: unknown arguments: dunno=this",
                assertThrows(Exception.class, () -> cl.setFailOnUnknownArg(true)
                        .parse("--dunno this")).getMessage());
    }

    @Test
    public void parseUnknownToken() {
        Option<Integer> optInt = Option.builder(int.class)
                .name("optInt")
                .optional()
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .setFailOnUnknownToken(false)
                .parse("--optInt 1 2 3");
        assertFalse(cl.isFailOnUnknownToken());
        assertCollection(cl.getUnknownTokens(), "2", "3");

        assertEquals("Parsing of command-line failed: unknown tokens: 2, 3",
                assertThrows(Exception.class, () -> cl.setFailOnUnknownToken(true)
                        .parse("--optInt 1 2 3")).getMessage());
    }

    @Test
    public void parseDupArg() {
        Option<Void> optDup = Option.builder()
                .name("optdup")
                .optional()
                .description("optional int")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(optDup)
                .setFailOnDupArg(false)
                .parse("--optdup --optdup");
        assertFalse(cl.isFailOnDupArg());
        assertMap(cl.getDupArgs(), "optdup");

        assertEquals("Parsing of command-line failed: duplicate arguments: optdup",
                assertThrows(Exception.class, () -> cl.setFailOnDupArg(true)
                        .parse("--optdup --optdup")).getMessage());
    }

    @Test
    public void parseRequiredOptionMissingFail() {
        CommandLine cl = new CommandLine().addOption(Option.builder(String.class)
                .name("requiredString")
                .required()
                .build());
        assertEquals("Parsing of command-line failed: required options missing: requiredString",
                assertThrows(Exception.class, () -> cl.parse("")).getMessage());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"true", " true ", "TRUE", "1", "yes"})
    public void parseBooleanArgTrue(String _boolArg) {
        Option<Boolean> optBool = Option.builder(boolean.class)
                .name("optBool")
                .build();
        CommandLine cl = new CommandLine().addOption(optBool);
        assertTrue(cl.parse(new String[] {"--" + optBool.getName(), _boolArg}).getArg(optBool));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "false", "FALSE", "0", "no"})
    public void parseBooleanArgFalse(String _boolArg) {
        Option<Boolean> optBool = Option.builder(boolean.class)
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
        Option<LocalDate> optDate = Option.builder(LocalDate.class)
                .name("optDate")
                .required()
                .build();
        CommandLine cl = new CommandLine().addOption(optDate);
        assertEquals("Parsing of command-line failed: argument 'optDate' requires a value",
                assertThrows(CommandLineException.class, () -> cl.parse(new String[] {"--" + optDate.getName(), _dateArg}).getArg(optDate)).getMessage());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"xy", "9999-88-77", "66665544"})
    public void parseDateArgInvalid(String _dateArg) {
        Option<LocalDate> optDate = Option.builder(LocalDate.class)
                .name("optDate")
                .required()
                .description("descr")
                .build();
        CommandLine cl = new CommandLine().addOption(optDate);
        CommandLineException ex = assertThrows(CommandLineException.class, () -> cl.parse(new String[] {"--optDate", _dateArg}));
        assertEquals("Parsing of command-line failed: argument 'optDate' has invalid value (" + _dateArg + ")", ex.getMessage());
    }

    @Test
    public void parseDateArgDefault() {
        LocalDate defLd = LocalDate.now();
        Option<LocalDate> optLd = Option.builder(LocalDate.class)
                .name("optDate")
                .defaultValue(defLd)
                .build();

        LocalDateTime defLdt = LocalDateTime.now();
        Option<LocalDateTime> optLdt = Option.builder(LocalDateTime.class)
                .name("optDateTime")
                .defaultValue(defLdt)
                .build();

        CommandLine cl = new CommandLine().addOptions(optLd, optLdt).parse("--optDate --optDateTime");
        assertEquals(defLd, cl.getArg(optLd));
        assertEquals(defLdt, cl.getArg(optLdt));
    }

    @Test
    public void getHelpNoArgs() {
        assertPatternMatches(new CommandLine().getHelp(null), "^usage: [^ ]+\n");
    }

    @Test
    public void getHelpWithArgs() {
        CommandLine cl = new CommandLine()
                .addOption(Option.builder(String.class)
                        .name("req1")
                        .required()
                        .defaultValue("default")
                        .build())
                .addOption(Option.builder()
                        .name("opt1")
                        .optional()
                        .build());
        assertEquals("usage: myApp --req1 <arg> [--opt1]" + System.lineSeparator(), cl.getHelp("myApp"));
    }

    @Test
    public void printHelp() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));
        new CommandLine().printHelp("myApp");
        System.setOut(oldOut);

        String help = baos.toString();
        assertTrue(help.startsWith("usage: myApp" + System.lineSeparator()));
    }

}

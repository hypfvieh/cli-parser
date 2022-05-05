package com.github.hypfvieh.cli.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
    public void addSameOptionMoreThanOnce() {
        String optName = "arg1";
        CommandLine cl = new CommandLine()
                .addOption(CmdArgOption.builder(String.class)
                        .name(optName)
                        .optional()
                        .defaultValue("default1")
                        .build());

        assertFalse(cl.getOption(optName).isRequired());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            cl.addOption(CmdArgOption.builder(String.class)
                    .name(optName)
                    .required()
                    .defaultValue("default2")
                    .build());
        });

        assertEquals("Command-line option '--arg1' already defined", ex.getMessage());
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
    public void getOptionNoValue() {
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
    public void getOptionNoValueWithValue() {
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
    public void parseShortOptions() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName("o")
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
                .shortName("o")
                .optional()
                .description("optional int")
                .build();
        CmdArgOption<?> optAll = CmdArgOption.builder()
                .shortName("a")
                .optional()
                .description("all flag")
                .build();
        
        CommandLine cl = new CommandLine()
                .addOption(optInt)
                .addOption(optAll)
                .withFailOnUnknownToken(false)
                .parse("-ao 1 2 3");
        
        assertFalse(cl.isFailOnUnknownToken());
        assertCollection(cl.getUnknownTokens(), "2", "3");
        
        assertTrue(cl.hasArg(optAll));
        assertTrue(cl.hasArg(optInt));
    }
    
    @Test
    public void parseMultiOptions() {
        CmdArgOption<Integer> optInt = CmdArgOption.builder(int.class)
                .shortName("o")
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
                .shortName("o")
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

        CommandLine cl = new CommandLine()
               .addOptions(optBoolSimple, optBoolWrapper, optByte, optShort)
               .addOptions(optIntReq, optIntOpt)
               .addOptions(optLong, optFloat, optDouble)
               .addOptions(optString, optLocalDate);

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
               , "--optLocalDate 2025-04-07"));

        cl.getOptions().values().stream().filter(CmdArgOption::isRequired).map(cl::hasArg).forEach(CommandLineTest::assertTrue);

        assertEquals(LocalDate.of(2025, 4, 7), cl.getArg(optLocalDate));
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
    public void getHelpNoArgs() {
        assertPatternMatches(new CommandLine().getUsage(null), "^usage: [^ ]+\n");
    }

    @Test
    public void getHelpWithArgs() {
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
    public void printHelp() throws IOException {
        
        String help;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); var ps = new PrintStream(baos)) {
            new CommandLine().printUsage("myApp", new PrintStream(baos));
            help = baos.toString();
        }
        assertTrue(help.startsWith("usage: myApp" + System.lineSeparator()));
    }

}

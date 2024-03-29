package com.github.hypfvieh.cli.parser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Abstract base class for unit tests.<p>
 *
 * Logs entry and exit to/from all test methods.<br>
 * This class extends JUnit assertions to avoid the need for static imports in subclasses.
 *
 * @author Markus Spann
 */
public abstract class AbstractBaseTest extends Assertions {

    /** Instance logger. */
    private Logger   logger;

    /** Holds information about the current test. */
    private TestInfo lastTestInfo;

    /** Start time of test in milliseconds. */
    private long     startTime;

    protected final Logger getLogger() {
        if (null == logger) {
            logger = System.getLogger(getClass().getName());
        }
        return logger;
    }

    protected final void setLogger(String _loggerName) {
        logger = System.getLogger(Objects.requireNonNull(_loggerName, "Logger name required"));
    }

    @BeforeEach
    public final void setTestMethodName(TestInfo _testInfo) {
        lastTestInfo = _testInfo;
    }

    protected final String getTestMethodName() {
        if (lastTestInfo != null && lastTestInfo.getTestClass().isPresent()) {
            return lastTestInfo.getTestClass().get().getName() + '.' + lastTestInfo.getTestMethod().get().getName();
        }
        return null;
    }

    protected final String getShortTestMethodName() {
        Optional<Method> testMethod = lastTestInfo == null ? Optional.empty() : lastTestInfo.getTestMethod();
        return testMethod.map(Method::getName).orElse(null);
    }

    @BeforeEach
    public final void logTestBegin(TestInfo _testInfo) {
        startTime = System.currentTimeMillis();
        String name = _testInfo.getTestMethod().map(Method::getName).orElse(null);
        if (name == null || _testInfo.getDisplayName().startsWith(name)) {
            getLogger().log(Level.INFO, ">>>>>>>>>> BGN Test: {0} <<<<<<<<<<", _testInfo.getDisplayName());
        } else {
            getLogger().log(Level.INFO, ">>>>>>>>>> BGN Test: {0} ({1}) <<<<<<<<<<", name, _testInfo.getDisplayName());
        }
    }

    @AfterEach
    public final void logTestEnd(TestInfo _testInfo) {
        String name = _testInfo.getTestMethod().map(Method::getName).orElse(null);
        String seconds = String.format("%.2f sec", (System.currentTimeMillis() - startTime) / 1000d);
        if (name == null || _testInfo.getDisplayName().startsWith(name)) {
            getLogger().log(Level.INFO, ">>>>>>>>>> END Test: {0} ({1}) <<<<<<<<<<", _testInfo.getDisplayName(), seconds);
        } else {
            getLogger().log(Level.INFO, ">>>>>>>>>> END Test: {0} ({1}) ({2}) <<<<<<<<<<", name, _testInfo.getDisplayName(), seconds);
        }
    }

    /**
     * Retrieves class name and method name at the specified stacktrace index.
     *
     * @param _index stacktrace index
     * @return fully qualified method name
     */
    private static String getStackTraceString(int _index) {
        StackTraceElement[] arrStackTraceElems = new Throwable().fillInStackTrace().getStackTrace();
        final int lIndex = Math.min(arrStackTraceElems.length - 1, Math.max(0, _index));
        return arrStackTraceElems[lIndex].getClassName() + "." + arrStackTraceElems[lIndex].getMethodName();
    }

    /**
     * Gets the current method name.
     *
     * @return method name
     */
    public static String getMethodName() {
        return getStackTraceString(2);
    }

    /**
     * Gets the calling method name.
     *
     * @return method name
     */
    public static String getCallingMethodName() {
        return getStackTraceString(3);
    }

    private static String getExceptionAsString(Throwable _ex) {
        if (_ex == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (_ex.getStackTrace() == null) {
            _ex.fillInStackTrace();
        }
        _ex.printStackTrace(new PrintStream(baos));
        return baos.toString();
    }

    public static void assertEmpty(String _string) {
        assertTrue(_string != null && _string.isEmpty(), "String not empty.");
    }

    public static void assertNotEmpty(String _string) {
        assertTrue(_string == null || _string.isEmpty(), "String is empty.");
    }

    public static void assertBlank(String _string) {
        assertTrue(_string != null && _string.isBlank(), "String not blank.");
    }

    public static void assertNotBlank(String _string) {
        assertTrue(_string == null || _string.isBlank(), "String is blank.");
    }

    public static void assertContains(String _string, String _contains) {
        if (_contains != null) {
            assertTrue(_string != null && _string.contains(_contains), "String does not contain [" + _contains + "]: " + _string);
        }
    }

    public static void assertContainsNot(String _string, String _notContains) {
        if (_notContains != null) {
            assertFalse(_string == null || _string.contains(_notContains), "String contains [" + _notContains + "]: " + _string);
        }
    }

    public static void assertDoubleEquals(double _expected, double _actual) {
        assertEquals(_expected, _actual, 0.000001d);
    }

    /**
     * Fails a test with the given message and optional exception.
     *
     * @param _message message
     * @param _ex exception
     */
    public static void assertFail(String _message, Throwable _ex) {
        String message = _message == null || _message.isBlank() ? "!no fail message provided by " + getCallingMethodName() : _message;
        if (_ex != null) {
            message += " " + getExceptionAsString(_ex);
        }
        fail(message);
    }

    public static void assertFail(String _message) {
        assertFail(_message, null);
    }

    /**
     * Asserts that the specified file exists.
     *
     * @param _file file object
     * @return the file object
     */
    public static final File assertFileExists(File _file) {
        return assertFileExists(_file, true);
    }

    public static final File assertFileNotExists(File _file) {
        return assertFileExists(_file, false);
    }

    /**
     * Asserts that the specified file exists or does not exists.
     *
     * @param _file file object
     * @param _exists true if should exist, false otherwise
     * @return the file object
     */
    private static File assertFileExists(File _file, boolean _exists) {
        assertNotNull(_file, "File object is null.");
        if (_exists) {
            assertTrue(_file.exists(), "File [" + _file.getAbsolutePath() + "] does not exist.");
        } else {
            assertFalse(_file.exists(), "File [" + _file.getAbsolutePath() + "] exists.");
        }
        return _file;
    }

    public static final File assertFileExists(String _file) {
        assertNotNull(_file);
        return assertFileExists(new File(_file));
    }

    /**
     * Asserts that the specified environment variable is set.
     *
     * @param _name environment variable name
     * @return value of environment variable
     */
    public static final String assertEnvSet(String _name) {
        assertNotNull(_name);
        String value = System.getenv(_name);
        assertNotEmpty(value);
        return value;
    }

    /**
     * Asserts the specified map is non-null and contains all of the given keys.
     *
     * @param _map map
     * @param _keys array of keys, may be null or zero length
     * @return the map parameter
     */
    @SafeVarargs
    public static final <K, V> Map<K, V> assertMap(Map<K, V> _map, K... _keys) {
        assertNotNull(_map, "Map is null.");
        if (_keys != null) {
            for (Object key : _keys) {
                assertTrue(_map.containsKey(key), "Key [" + key + "] not found in map: " + _map);
            }
        }
        return _map;
    }

    /**
     * Asserts the specified collection is non-null and contains all of the given values.
     *
     * @param _coll collection
     * @param _values array of values, may be null or zero length
     * @return the collection parameter
     */
    @SafeVarargs
    public static final <V> Collection<V> assertCollection(Collection<V> _coll, V... _values) {
        assertNotNull(_coll, "Collection is null.");
        Collection<V> notFound = new ArrayList<>();
        if (_values != null) {
            for (V val : _values) {
                if (!_coll.contains(val)) {
                    notFound.add(val);
                }
            }
        }
        assertTrue(notFound.isEmpty(), "Values " + notFound + " not found in collection: " + _coll);
        return _coll;
    }

    public static void assertInstanceOf(Object _obj, Class<?> _class) {
        assertTrue(_obj != null && _class != null && _class.isAssignableFrom(_obj.getClass()), _obj + " is not an instance of " + _class + ".");
    }

    public static final void assertPatternFind(String _str, String _pattern) {
        assertNotNull(_str, "String may not be null.");
        assertNotNull(_pattern, "Pattern may not be null.");
        assertTrue(Pattern.compile(_pattern).matcher(_str).find(), "Pattern [" + _pattern + "] not found in string [" + _str + "].");
    }

    public static final void assertPatternMatches(String _str, String _pattern) {
        assertNotNull(_str, "String may not be null.");
        assertNotNull(_pattern, "Pattern may not be null.");
        assertTrue(Pattern.compile(_pattern).matcher(_str).matches(), "Pattern [" + _pattern + "] does not match string [" + _str + "].");
    }
}

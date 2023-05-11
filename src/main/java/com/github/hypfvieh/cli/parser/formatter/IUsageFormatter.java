package com.github.hypfvieh.cli.parser.formatter;

import com.github.hypfvieh.cli.parser.AbstractBaseCommandLine;
import com.github.hypfvieh.cli.parser.CmdArgOption;

import java.util.Arrays;
import java.util.List;

/**
 * Interface which have to be implemented by any usage formatter.
 *
 * @author David M.
 * @author Markus S.
 * @since 1.0.0 - 2022-05-05
 */
@FunctionalInterface
public interface IUsageFormatter {

    /**
     * Called to retrieve a formatted usage output.
     * <p>
     * Output should be properly formatted including line-feeds etc.
     * </p>
     *
     * @param _options registered options, maybe empty - never null
     * @param _longOptPrefix prefix for long options
     * @param _shortOptPrefix prefix for short options
     * @param _mainClassName main class name, maybe null
     *
     * @return formatted String
     */
    String format(List<CmdArgOption<?>> _options, String _longOptPrefix, String _shortOptPrefix, String _mainClassName);

    /**
     * Returns the simple class name of the topmost stack element which is not in our own package.
     *
     * @return simple class name
     */
    static String getMainClassName() {
        StackTraceElement[] stackTrace = new Throwable().fillInStackTrace().getStackTrace();

        String mainClassName = Arrays.stream(stackTrace)
            .map(StackTraceElement::getClassName)
            .filter(st -> !st.startsWith("java.") && !st.startsWith("javax.") && !st.startsWith(AbstractBaseCommandLine.class.getPackageName()))
            .findFirst()
            .orElse("Unknown");

        int idx = mainClassName.lastIndexOf('.');
        if (idx > -1) {
            mainClassName = mainClassName.substring(idx + 1);
        }

        return mainClassName;
    }
}

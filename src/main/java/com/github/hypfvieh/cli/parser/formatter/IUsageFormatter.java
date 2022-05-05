package com.github.hypfvieh.cli.parser.formatter;

import java.util.Arrays;
import java.util.List;

import com.github.hypfvieh.cli.parser.AbstractBaseCommandLine;
import com.github.hypfvieh.cli.parser.CmdArgOption;

/**
 * Interface which have to be implemented by any usage formatter.
 * 
 * @author hypfvieh
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
     * @param _mainClassName main class name, maybe null
     * 
     * @return formatted String
     */
    public String format(List<CmdArgOption<?>> _options, String _mainClassName);
    
    /**
     * Returns the simple class name of the topmost stack element which is not in our own package.
     * 
     * @return simple class name
     */
    static String getMainClassName() {
        StackTraceElement[] stackTrace = new Throwable().fillInStackTrace().getStackTrace();

        String mainClassName = Arrays.stream(stackTrace)
            .map(st -> st.getClassName())
            .filter(st -> st.startsWith(AbstractBaseCommandLine.class.getPackageName()))
            .findFirst()
            .orElse("Unknown");
        
        int idx = mainClassName.lastIndexOf(".");
        if (idx > -1) {
            mainClassName = mainClassName.substring(idx + 1);
        }
        
        return mainClassName;
    }
}

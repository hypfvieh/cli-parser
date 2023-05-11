### TODOs

    - [OK] Support multi short-options (e.g. -vvv or -v -v -v) like openssh to increase debug level based on given -v count
    - [OK] Support values delimited by = (e.g. --myOption=bla, -p=blubb)
    - [OK] Increase test coverage
    - [OK] Add missing javadoc
    - [OK] Check uniqueness of all long and short options (prevent conflicting duplicates)
    - [OK] Apply Anyedit or similar to prevent trailing whitespaces at end of line and on empty lines
    - [OK] Introduce static code analysis, fail build on serious violations
    - [  ] Inherit from parent POM org.basepom:basepom-minimal
    - [  ] Consider scenario: Required option, value is present but converter returns null
    - [OK] Use specific run-time exceptions e.g. CommandLineException rather than IllegalArgumentException etc.
    - [  ] Prevent CmdArgOption.getDataType() from being null by introducing a default data type e.g. String

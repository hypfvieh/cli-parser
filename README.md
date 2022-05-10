# cli-parser
Utility library to provide support for commandline parsing.
It supports long (e.g. --my-option) and short options (e.g. -o) as well as Equal-Style options (.e.g. --my-option=value or -o=value).

It is possible to change the long and short option prefix (`withLongOptPrefix(String)` / `withShortOptPrefix(String)`).
You can set various options to handle unknown, incomplete or wrong parameters.

You may register type converters (`registerConverter(Class<T>, IValueConverter<T>)`) to support convertion from input
parameter value to a certain java type.
The cli-parser provides some default converters for LocalDate/LocalDateTime/LocalTime, Double, Integer and Long.

# Usage

## Include dependency in your project
```xml
<dependency>
    <groupId>com.github.hypfvieh.cli</groupId>
    <artifactId>cli-parser</artifactId>
    <version>1.0.0</version>
</dependency>
```
## Code Usage
Simple Usage looks like this:

```java
public class MyMainApp {
  public void main(String[] _args) {
     CommandLine cl = new CommandLine()
                .addOption(CmdArgOption.builder(String.class)
                        .name("optionWithValue")
                        .shortName('f')
                        .required(true)
                        .description("descr")
                        .build())
                .parse(_args);

        Object val = cl.getArg('f');

        System.out.println("Given value was: " + val);
  }
}
```

Using options value type looks like this:

```java
public class MyMainApp {
  public void main(String[] _args) {
     CmdArgOption<String> stringOption = CmdArgOption.builder(String.class)
                .name("optionWithValue")
                .shortName('f')
                .required(true)
                .description("descr")
                .build();
        CommandLine cl = new CommandLine()
                .addOption(stringOption)
                .parse(_args);

        String val = cl.getArg(stringOption);

        System.out.println("Given value was: " + val);
  }
}
```
## Options
Options define a supported parameter with an optional value and return type.
The CmdArgOption uses the builder pattern to create options. 

Options can be repeatable which means that they may be used multiple times.
When the option is repeatable and requires a value, all received values can be retrieved by using `getArgs(Option<T>)`.
If you use `getArg(Option<T>)` when multiple values are set, you will only get the first given value.

If an option is repeatable but does not take any value, you can get the "repeat count" by using `getArgCount()`.

Options can be optional or required. If an option is required and is missing `CommandLine` will throw a `RuntimeException`
(`CommandLineException` by default). The exception type can be changed on the `CommandLine` object.

## Usage Formatter
CommandLine supports custom usage formatters. 
The Usage formatter will be used to print the supported options (required or optional) when the given command line was invalid.

If the default usage formatter does not fit your needs, you can implement your own.
Create a class implementing `IUsageFormatter` and set a new instance of this formatter in your CommandLine object using `withUsageFormatter(IUsageFormatter)`.

## Creating Converters
To create a converter you have to create a class which implements `IValueConverter`.
When conversation fails, the converter should throw a `CommandLineException` (or a subclass of it).
The new convert has to be registered using `registerConverter(Class<T>, IValueConverter<T>)` on the `CommandLine` object.

### Sample converter:
```java
public class FloatConverter implements IValueConverter<Float> {

    @Override
    public Double convert(String _string) {
        try {
            return Float.parseFloat(_string);
        } catch (NumberFormatException _ex) {
            throw new CommandLineException("Unable to parse input '" + _string + "' as float");
        }
    }

}
```

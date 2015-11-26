package runtime;

import java.math.BigInteger;

public class OutputFormatter {

    public static String output(boolean value) {
        return value ? "#t" : "#f";
    }

    public static String output(char character) {
        switch (character) {
            case '\n':
                return "#\\newline";
            case ' ':
                return "#\\space";
            default:
                return String.valueOf(character);
        }
    }

    public static String output(String string) {
        return string;
    }

    public static String output(BigInteger bigInteger) {
        return String.valueOf(bigInteger);
    }
}

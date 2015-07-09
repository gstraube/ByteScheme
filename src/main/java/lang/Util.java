package lang;

import org.antlr.v4.runtime.misc.ParseCancellationException;

public class Util {

    public static void checkExactArity(int argumentsCount, int arity) {
        if (argumentsCount != arity) {
            String message = "Arguments count %d does not match expected arity of %d";
            throw new ParseCancellationException(String.format(message, argumentsCount, arity));
        }
    }

}

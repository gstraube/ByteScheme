package lang;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PredefinedProcedures {

    public static final Map<String, Procedure> MATH_PROCEDURES = new HashMap<>();

    static {
        defineAddition();
        defineSubtraction();
        defineMultiplication();
        defineQuotient();
    }

    private static void defineQuotient() {
        MATH_PROCEDURES.put("quotient", arguments -> {
            checkExactArity(arguments.size(), 2);
            int dividend = Integer.parseInt(arguments.get(0));
            int divisor = Integer.parseInt(arguments.get(1));

            return String.valueOf(dividend / divisor);
        });
    }

    private static void defineMultiplication() {
        MATH_PROCEDURES.put("*", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            return arguments.stream()
                    .map(Integer::valueOf)
                    .reduce(1, (a, b) -> a * b)
                    .toString();
        });
    }

    private static void defineSubtraction() {
        MATH_PROCEDURES.put("-", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<Integer> intArguments = arguments.stream()
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            Integer firstArgument = intArguments.stream().findFirst().get();
            List<Integer> tailArguments = intArguments.subList(1, intArguments.size());

            if (tailArguments.isEmpty()) {
                return String.valueOf(-firstArgument);
            }

            return tailArguments.stream()
                    .reduce(firstArgument, (a, b) -> a - b)
                    .toString();
        });
    }

    private static void defineAddition() {
        MATH_PROCEDURES.put("+", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            return arguments.stream()
                    .map(Integer::valueOf)
                    .reduce(0, (a, b) -> a + b)
                    .toString();
        });
    }

    private static void checkExactArity(int argumentsCount, int arity) {
        if (argumentsCount != arity) {
            String message = "Arguments count %d does not match expected arity of %d";
            throw new ParseCancellationException(String.format(message, argumentsCount, arity));
        }
    }

    private static void checkMinimalArity(int argumentsCount, int minimalArity) {
        if (argumentsCount < minimalArity) {
            String message = "Arguments count %d does not match expected minimal arity of %d";
            throw new ParseCancellationException(String.format(message, argumentsCount, minimalArity));
        }
    }

}

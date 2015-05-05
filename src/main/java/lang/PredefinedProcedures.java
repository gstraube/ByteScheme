package lang;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            List<Integer> intArguments = castToIntArguments(arguments);
            int dividend = intArguments.get(0);
            int divisor = intArguments.get(1);
            int result = dividend / divisor;

            return new Constant<>(result, String.valueOf(result));
        });
    }

    private static void defineMultiplication() {
        MATH_PROCEDURES.put("*", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<Integer> intArguments = castToIntArguments(arguments);
            int product = intArguments.stream().reduce(1, (a, b) -> a * b);
            return new Constant<>(product, String.valueOf(product));
        });
    }

    private static void defineSubtraction() {
        MATH_PROCEDURES.put("-", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<Integer> intArguments = castToIntArguments(arguments);
            Integer firstArgument = intArguments.stream().findFirst().get();
            List<Integer> tailArguments = intArguments.subList(1, intArguments.size());
            int result;

            if (tailArguments.isEmpty()) {
                result = -firstArgument;
            } else {
                result = tailArguments.stream().reduce(firstArgument, (a, b) -> a - b);
            }

            return new Constant<>(result, String.valueOf(result));
        });
    }

    private static void defineAddition() {
        MATH_PROCEDURES.put("+", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<Integer> intArguments = castToIntArguments(arguments);
            int sum =  intArguments.stream().reduce(0, (a, b) -> a + b);
            return new Constant<>(sum, String.valueOf(sum));
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

    private static List<Integer> castToIntArguments(List<Datum> arguments) {
        List<Integer> intArguments = new ArrayList<>();
        for (Datum datum : arguments) {
            Constant<Integer> integerConstant = (Constant<Integer>) datum;
            intArguments.add(integerConstant.getValue());
        }
        return intArguments;
    }

}

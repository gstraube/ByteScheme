package lang;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.*;

public abstract class PredefinedProcedures {

    public static final Map<String, Procedure> MATH_PROCEDURES = new HashMap<>();
    public static final Map<String, Procedure> LIST_PROCEDURES = new HashMap<>();
    public static final Map<String, Procedure> EQUALITY_PROCEDURES = new HashMap<>();
    public static final Map<String, Procedure> CONDITIONALS = new HashMap<>();
    public static final Map<String, Procedure> NUMBER_COMPARATORS = new HashMap<>();

    static {
        defineCar();
        defineCdr();
        defineAddition();
        defineSubtraction();
        defineMultiplication();
        defineQuotient();
        defineNumberComparators();
        defineEquality();
        defineIfStatement();
    }

    private static void defineNumberComparators() {
        NUMBER_COMPARATORS.put("<", arguments -> {
            checkExactArity(arguments.size(), 2);
            List<Integer> integers = castToIntArguments(arguments);
            if (integers.get(0) < integers.get(1)) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
        NUMBER_COMPARATORS.put("<=", arguments -> {
            checkExactArity(arguments.size(), 2);
            List<Integer> integers = castToIntArguments(arguments);
            if (integers.get(0) <= integers.get(1)) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
        NUMBER_COMPARATORS.put(">", arguments -> {
            checkExactArity(arguments.size(), 2);
            List<Integer> integers = castToIntArguments(arguments);
            if (integers.get(0) > integers.get(1)) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
        NUMBER_COMPARATORS.put(">=", arguments -> {
            checkExactArity(arguments.size(), 2);
            List<Integer> integers = castToIntArguments(arguments);
            if (integers.get(0) >= integers.get(1)) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
    }

    private static void defineIfStatement() {
        CONDITIONALS.put("if", arguments -> {
            checkExactArity(arguments.size(), 3);

            boolean hasBooleanCondition = false;
            boolean condition = true;

            Datum firstArgument = arguments.get(0);
            if (firstArgument instanceof Constant) {
                Constant constant = (Constant) firstArgument;
                if (constant.getValue() instanceof Boolean) {
                    hasBooleanCondition = true;
                    condition = (Boolean) constant.getValue();
                }
            }

            /*
                The value of the third argument (the else branch) is only
                returned when the first argument evaluates to #f.

                An except from R5RS standard (http://www.schemers.org/Documents/Standards/R5RS/,
                section "6.3.1 Booleans"):

                "Of all the standard Scheme values, only #f counts as false in conditional expressions.
                 Except for #f, all standard Scheme values, including #t, pairs, the empty list, symbols,
                 numbers, strings, vectors, and procedures, count as true."
             */
            if (!hasBooleanCondition || condition) {
                return arguments.get(1);
            } else {
                return arguments.get(2);
            }
        });
    }

    private static void defineEquality() {
        EQUALITY_PROCEDURES.put("equal?", new Procedure() {
            @Override
            public Datum apply(List<Datum> arguments) {
                checkExactArity(arguments.size(), 2);

                Datum firstArgument = arguments.get(0);
                Datum secondArgument = arguments.get(1);

                if (!firstArgument.getType().equals(secondArgument.getType())) {
                    return new Constant<>(false, "#f");
                }

                if (firstArgument instanceof Constant) {
                    Constant firstConstant = (Constant) firstArgument;
                    Constant secondConstant = (Constant) secondArgument;
                    boolean isEqual = firstConstant.getValue().equals(secondConstant.getValue());
                    return isEqual ? new Constant<>(true, "#t") : new Constant<>(false, "#f");
                }

                if (firstArgument instanceof Sequence) {
                    List<Datum> firstArgElements = ((Sequence) firstArgument).getElements();
                    List<Datum> secondArgElements = ((Sequence) secondArgument).getElements();

                    if (firstArgElements.size() != secondArgElements.size()) {
                        return new Constant<>(true, "#f");
                    }

                    int i = 0;
                    for (Datum element : firstArgElements) {
                        Constant<Boolean> result = (Constant<Boolean>) this.apply(Arrays.asList(element,
                                secondArgElements.get(i)));
                        if (!result.getValue()) {
                            return result;
                        }
                        i++;
                    }

                    return new Constant<>(true, "#t");
                }

                return new Constant<>(true, "#f");
            }
        });

    }

    private static void defineCar() {
        LIST_PROCEDURES.put("car", arguments -> {
            checkExactArity(arguments.size(), 1);
            SList list = castToList(arguments);
            if (list.length() == 0) {
                throw new ParseCancellationException("Wrong argument type: Expected pair");
            }
            return list.car();
        });
    }

    private static void defineCdr() {
        LIST_PROCEDURES.put("cdr", arguments -> {
            checkExactArity(arguments.size(), 1);
            SList list = castToList(arguments);
            if (list.length() == 0) {
                throw new ParseCancellationException("Wrong argument type: Expected pair");
            }
            return list.cdr();
        });
    }

    private static SList castToList(List<Datum> arguments) {
        Datum argument = arguments.get(0);
        if (!(argument instanceof SList)) {
            throw new ParseCancellationException("Wrong argument type: Expected pair");
        }
        return (SList) arguments.get(0);
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
            int sum = intArguments.stream().reduce(0, (a, b) -> a + b);
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

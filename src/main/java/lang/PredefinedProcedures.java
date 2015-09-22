package lang;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.math.BigInteger;
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
    }

    private static void defineNumberComparators() {
        NUMBER_COMPARATORS.put("<", new Procedure() {
            @Override
            public Datum apply(List<Datum> arguments) {
                Util.checkExactArity(arguments.size(), 2);
                List<BigInteger> integers = castToIntArguments(arguments);
                int comparison = integers.get(0).compareTo(integers.get(1));
                if (comparison == -1) {
                    return new Constant<>(true, "#t");
                }
                return new Constant<>(false, "#f");
            }
        });
        NUMBER_COMPARATORS.put("<=", arguments -> {
            Util.checkExactArity(arguments.size(), 2);
            List<BigInteger> integers = castToIntArguments(arguments);
            int comparison = integers.get(0).compareTo(integers.get(1));
            if (comparison == -1 || comparison == 0) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
        NUMBER_COMPARATORS.put(">", arguments -> {
            Util.checkExactArity(arguments.size(), 2);
            List<BigInteger> integers = castToIntArguments(arguments);
            int comparison = integers.get(0).compareTo(integers.get(1));
            if (comparison == 1) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
        NUMBER_COMPARATORS.put(">=", arguments -> {
            Util.checkExactArity(arguments.size(), 2);
            List<BigInteger> integers = castToIntArguments(arguments);
            int comparison = integers.get(0).compareTo(integers.get(1));
            if (comparison == 0 || comparison == 1) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
        NUMBER_COMPARATORS.put("=", arguments -> {
            Util.checkExactArity(arguments.size(), 2);
            List<BigInteger> integers = castToIntArguments(arguments);
            int comparison = integers.get(0).compareTo(integers.get(1));
            if (comparison == 0) {
                return new Constant<>(true, "#t");
            }
            return new Constant<>(false, "#f");
        });
    }

    private static void defineEquality() {
        EQUALITY_PROCEDURES.put("equal?", new Procedure() {
            @Override
            public Datum apply(List<Datum> arguments) {
                Util.checkExactArity(arguments.size(), 2);

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
            Util.checkExactArity(arguments.size(), 1);
            SList list = castToList(arguments);
            if (list.length() == 0) {
                throw new ParseCancellationException("Wrong argument type: Expected pair");
            }
            return list.car();
        });
    }

    private static void defineCdr() {
        LIST_PROCEDURES.put("cdr", arguments -> {
            Util.checkExactArity(arguments.size(), 1);
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
            Util.checkExactArity(arguments.size(), 2);
            List<BigInteger> intArguments = castToIntArguments(arguments);
            BigInteger dividend = intArguments.get(0);
            BigInteger divisor = intArguments.get(1);
            BigInteger result = dividend.divide(divisor);

            return new Constant<>(result, String.valueOf(result));
        });
    }

    private static void defineMultiplication() {
        MATH_PROCEDURES.put("*", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<BigInteger> intArguments = castToIntArguments(arguments);
            BigInteger product = intArguments.stream().reduce(BigInteger.ONE, BigInteger::multiply);
            return new Constant<>(product, String.valueOf(product));
        });
    }

    private static void defineSubtraction() {
        MATH_PROCEDURES.put("-", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<BigInteger> intArguments = castToIntArguments(arguments);
            BigInteger firstArgument = intArguments.stream().findFirst().get();
            List<BigInteger> tailArguments = intArguments.subList(1, intArguments.size());
            BigInteger result;

            if (tailArguments.isEmpty()) {
                result = firstArgument.negate();
            } else {
                result = tailArguments.stream().reduce(firstArgument, BigInteger::subtract);
            }

            return new Constant<>(result, String.valueOf(result));
        });
    }

    private static void defineAddition() {
        MATH_PROCEDURES.put("+", arguments -> {
            checkMinimalArity(arguments.size(), 1);
            List<BigInteger> intArguments = castToIntArguments(arguments);
            BigInteger sum = intArguments.stream().reduce(BigInteger.ZERO, BigInteger::add);
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

    private static List<BigInteger> castToIntArguments(List<Datum> arguments) {
        List<BigInteger> intArguments = new ArrayList<>();
        for (Datum datum : arguments) {
            Constant<BigInteger> integerConstant = (Constant<BigInteger>) datum;
            intArguments.add(integerConstant.getValue());
        }
        return intArguments;
    }

}

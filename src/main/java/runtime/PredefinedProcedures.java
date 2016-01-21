package runtime;

import java.math.BigInteger;
import java.util.Arrays;

public class PredefinedProcedures {

    public static BigInteger add(Object[] arguments) {
        BigInteger sum = BigInteger.ZERO;
        for (Object argument : arguments) {
            java.math.BigInteger argumentAsBigInteger = (BigInteger) argument;
            sum = sum.add(argumentAsBigInteger);
        }

        return sum;
    }

    public static BigInteger subtract(Object[] arguments) {
        BigInteger firstArgument = (BigInteger) arguments[0];
        return firstArgument.subtract(add(Arrays.copyOfRange(arguments, 1, arguments.length)));
    }

    public static BigInteger negate(Object[] arguments) {
        return ((BigInteger) arguments[0]).negate();
    }

    public static BigInteger multiply(Object[] arguments) {
        BigInteger product = BigInteger.ONE;
        for (Object argument : arguments) {
            java.math.BigInteger argumentAsBigInteger = (BigInteger) argument;
            product = product.multiply(argumentAsBigInteger);
        }

        return product;
    }

    public static BigInteger divide(Object[] arguments) {
        BigInteger firstArgument = (BigInteger) arguments[0];
        BigInteger secondArgument = (BigInteger) arguments[1];

        return firstArgument.divide(secondArgument);
    }

}

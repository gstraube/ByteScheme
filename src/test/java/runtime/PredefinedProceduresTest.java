package runtime;

import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PredefinedProceduresTest {

    @Test
    public void integers_are_added_correctly() {
        assertThat(PredefinedProcedures.add(new Object[]{new BigInteger("2"),
                PredefinedProcedures.add(new Object[]{new BigInteger("10"), new BigInteger("5")}),
                new BigInteger("20")}), is(new BigInteger("37")));
    }

    @Test
    public void integers_are_subtracted_correctly() {
        assertThat(PredefinedProcedures.subtract(new Object[]{new BigInteger("10"),
                        PredefinedProcedures.subtract(new Object[]{new BigInteger("5"), new BigInteger("200")}),
                        new BigInteger("375"), PredefinedProcedures.negate(new Object[]{new BigInteger("20")})}),
                is(new BigInteger("-150")));
    }

    @Test
    public void integers_are_multiplied_correctly() {
        assertThat(PredefinedProcedures.multiply(new Object[]{new BigInteger("2"), new BigInteger("10"),
                        PredefinedProcedures.multiply(new Object[]{new BigInteger("3"), new BigInteger("7")})}),
                is(new BigInteger("420")));
    }

    @Test
    public void integers_are_divided_correctly() {
        assertThat(PredefinedProcedures.divide(new Object[]{new BigInteger("10"),
                        PredefinedProcedures.divide(new Object[]{new BigInteger("7"), new BigInteger("3")})}),
                is(new BigInteger("5")));
    }

}

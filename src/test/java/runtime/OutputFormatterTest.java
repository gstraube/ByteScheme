package runtime;

import lang.ListWrapper;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static runtime.OutputFormatter.output;

public class OutputFormatterTest {

    @Test
    public void boolean_values_are_formatted_correctly() {
        assertThat(output(true), is("#t"));
        assertThat(output(false), is("#f"));
    }

    @Test
    public void characters_are_formatted_correctly() {
        assertThat(output('a'), is("#\\a"));
        assertThat(output('\n'), is("#\\newline"));
        assertThat(output(' '), is("#\\space"));
    }

    @Test
    public void strings_are_formatted_correctly() {
        assertThat(output("a string"), is("a string"));
    }

    @Test
    public void integers_are_formatted_correctly() {
        assertThat(output(new BigInteger("469284342")), is("469284342"));
    }

    @Test
    public void a_list_is_formatted_correctly() {

        ListWrapper listWrapper = ListWrapper.fromElements(new BigInteger("15"),
                ListWrapper.fromElements("abc", true), new BigInteger("7"),
                ListWrapper.fromElements('u', false), new BigInteger("2"),
                "a_string");

        assertThat(output(listWrapper), is("(15 (abc #t) 7 (#\\u #f) 2 a_string)"));
    }

}
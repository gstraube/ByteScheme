package lang;

import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class ListWrapperTest {

    @Test
    public void car_returns_the_head_of_the_list() {
        ListWrapper listWrapper = ListWrapper.fromElements(new BigInteger("134343434"), "a string", true, 'a');
        assertThat(listWrapper.car(), is(new BigInteger("134343434")));
    }

    @Test
    public void cdr_returns_all_but_the_head_of_the_list() {
        ListWrapper listWrapper = ListWrapper.fromElements(new BigInteger("134343434"), "a string", true, 'a');
        ListWrapper tailListWrapper = ListWrapper.fromElements("a string", true, 'a');
        assertThat(listWrapper.cdr(), is(tailListWrapper));
    }

    @Test
    public void two_lists_are_equal_if_they_contain_the_same_elements_in_same_order() {
        ListWrapper listWrapper1 = ListWrapper.fromElements("a string", true, 'a');
        ListWrapper listWrapper2 = ListWrapper.fromElements("a string", true, 'a');
        ListWrapper listWrapper3 = ListWrapper.fromElements("another string", true, 'a');
        ListWrapper listWrapper4 = ListWrapper.fromElements(true, "a string", 'a');

        assertThat(listWrapper1, is(listWrapper2));
        assertThat(listWrapper1, not(is(listWrapper3)));
        assertThat(listWrapper1, not(is(listWrapper4)));
    }

}
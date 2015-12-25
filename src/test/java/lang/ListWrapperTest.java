package lang;

import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ListWrapperTest {

    @Test
    public void car_returns_the_head_of_the_list() {
        ListWrapper listWrapper =
                ListWrapper.fromElements(new Object[]{new BigInteger("134343434"), "a string", true, 'a'});
        assertThat(listWrapper.car(), is(new BigInteger("134343434")));
    }

    @Test
    public void cdr_returns_all_but_the_head_of_the_list() {
        ListWrapper listWrapper =
                ListWrapper.fromElements(new Object[]{new BigInteger("134343434"), "a string", true, 'a'});
        ListWrapper tailListWrapper = ListWrapper.fromElements(new Object[]{"a string", true, 'a'});
        assertThat(listWrapper.cdr(), is(tailListWrapper));
    }

    @Test
    public void two_lists_are_equal_if_they_contain_the_same_elements_in_same_order() {
        ListWrapper listWrapper1 = ListWrapper.fromElements(new Object[]{"a string", true, 'a'});
        ListWrapper listWrapper2 = ListWrapper.fromElements(new Object[]{"a string", true, 'a'});
        ListWrapper listWrapper3 = ListWrapper.fromElements(new Object[]{"another string", true, 'a'});
        ListWrapper listWrapper4 = ListWrapper.fromElements(new Object[]{true, "a string", 'a'});

        assertThat(listWrapper1, is(listWrapper2));
        assertThat(listWrapper1, not(is(listWrapper3)));
        assertThat(listWrapper1, not(is(listWrapper4)));
    }

    @Test
    public void lists_can_be_nested() {
        ListWrapper listWrapper = ListWrapper.fromElements(new Object[]{"a string",
                ListWrapper.fromElements(new Object[]{new BigInteger("439533232"), "another string"}), 'a'});

        Object element = listWrapper.cdr().car();

        assertThat(element, instanceOf(ListWrapper.class));

        ListWrapper innerList = (ListWrapper) element;

        assertThat(innerList.car(), instanceOf(BigInteger.class));
        assertThat(innerList.car(), is(new BigInteger("439533232")));

        assertThat(innerList.cdr(), instanceOf(ListWrapper.class));

        assertThat(innerList.cdr().car(), instanceOf(String.class));
        assertThat(innerList.cdr().car(), is("another string"));
    }

}
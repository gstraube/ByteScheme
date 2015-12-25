package lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListWrapper {

    private List<Object> elements;

    private ListWrapper(List<Object> elements) {
        this.elements = elements;
    }

    public static ListWrapper fromElements(Object[] elements) {
        return new ListWrapper(Arrays.asList(elements));
    }

    public Object car() {
        return elements.get(0);
    }

    public ListWrapper cdr() {
        return new ListWrapper(elements.subList(1, elements.size()));
    }

    public List<Object> getElements() {
        return new ArrayList<>(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ListWrapper that = (ListWrapper) o;

        return !(elements != null ? !elements.equals(that.elements) : that.elements != null);
    }

    @Override
    public int hashCode() {
        return elements != null ? elements.hashCode() : 0;
    }
}

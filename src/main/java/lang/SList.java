package lang;

import java.util.ArrayList;
import java.util.Collections;

public class SList implements Sequence {

    private java.util.List<Datum> elements;

    public SList(java.util.List<Datum> elements) {
        this.elements = new ArrayList<>();
        this.elements = Collections.unmodifiableList(elements);
    }

    @Override
    public String getText() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0; i < elements.size(); i++) {
            builder.append(elements.get(i).getText());
            if (i < elements.size() - 1) {
                builder.append(" ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

}

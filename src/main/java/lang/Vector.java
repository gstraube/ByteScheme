package lang;

import java.util.ArrayList;
import java.util.List;

public class Vector implements Sequence {

    private java.util.List<Datum> elements;

    public Vector(java.util.List<Datum> elements) {
        this.elements = new ArrayList<>();
        this.elements.addAll(elements);
    }

    @Override
    public String getText() {
        StringBuilder builder = new StringBuilder();
        builder.append("#(");
        for (int i = 0; i < elements.size(); i++) {
            builder.append(elements.get(i).getText());
            if (i < elements.size() - 1) {
                builder.append(" ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public List<Datum> getElements() {
        return elements;
    }
}

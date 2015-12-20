package runtime;

import lang.ListWrapper;

import java.math.BigInteger;
import java.util.List;

public class OutputFormatter {

    public static String output(Boolean value) {
        return value ? "#t" : "#f";
    }

    public static String output(Character character) {
        return String.valueOf(character);
    }

    public static String output(String string) {
        return string;
    }

    public static String output(BigInteger bigInteger) {
        return String.valueOf(bigInteger);
    }

    public static String output(ListWrapper listWrapper) {
        StringBuilder output = new StringBuilder();

        output.append("(");
        List<Object> elements = listWrapper.getElements();
        for (int i = 0; i < elements.size(); i++) {
            Object element = elements.get(i);
            if (element instanceof Boolean) {
                output.append(output((boolean) element));
            } else if (element instanceof Character) {
                output.append(output((char) element));
            } else if (element instanceof String) {
                output.append(output((String) element));
            } else if (element instanceof BigInteger) {
                output.append(output((BigInteger) element));
            } else if (element instanceof ListWrapper) {
                output.append(output((ListWrapper) element));
            }

            if (i < elements.size() - 1) {
                output.append(" ");
            }
        }
        output.append(")");

        return output.toString();
    }

}

package runtime;

import lang.ListWrapper;

import java.math.BigInteger;
import java.util.List;

public class OutputFormatter {

    public static String output(Boolean value) {
        return value ? "#t" : "#f";
    }

    public static String output(boolean value) {
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

    public static String output(Object object) {
        if (object instanceof Boolean) {
            return output((boolean) object);
        } else if (object instanceof Character) {
            return output((char) object);
        } else if (object instanceof String) {
            return output((String) object);
        } else if (object instanceof BigInteger) {
            return output((BigInteger) object);
        } else if (object instanceof ListWrapper) {
            return output((ListWrapper) object);
        }

        return "";
    }

    public static String output(ListWrapper listWrapper) {
        StringBuilder output = new StringBuilder();

        output.append("(");
        List<Object> elements = listWrapper.getElements();
        for (int i = 0; i < elements.size(); i++) {
            Object element = elements.get(i);
            output.append(output(element));

            if (i < elements.size() - 1) {
                output.append(" ");
            }
        }
        output.append(")");

        return output.toString();
    }

}

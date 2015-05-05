package lang;

public class Constant<T> implements Datum {

    private T value;
    private String text;

    public Constant(T value, String text) {
        this.value = value;
        this.text = text;
    }

    public T getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

}

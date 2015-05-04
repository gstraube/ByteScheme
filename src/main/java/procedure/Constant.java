package procedure;

public class Constant<T> {

    private T value;
    private String text;

    public Constant(T value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

}

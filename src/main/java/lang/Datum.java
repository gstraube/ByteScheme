package lang;

public interface Datum {

    public String getText();

    default String getType() {
        return getClass().getTypeName();
    }
}

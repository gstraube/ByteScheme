package lang;

public class Quotation implements Datum {

    public static final String QUOTATION_SYMBOL = "'";
    private Datum quotedValue;

    public Quotation(Datum quotedValue) {
        this.quotedValue = quotedValue;
    }

    @Override
    public String getText() {
        return QUOTATION_SYMBOL + quotedValue.getText();
    }
}

package lang;

public class Quotation implements Datum {

    private Datum quotedValue;

    public Quotation(Datum quotedValue) {
        this.quotedValue = quotedValue;
    }

    @Override
    public String getText() {
        String text = "";
        if (quotedValue instanceof Quotation) {
            text += "'";
        }
        return text + quotedValue.getText();
    }
}

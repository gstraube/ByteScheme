import java.util.ArrayList;
import java.util.List;

public class ParserTestVisitor extends SchemeBaseVisitor<Void> {

    List<Integer> constants = new ArrayList<>();

    @Override
    public Void visitConstant(SchemeParser.ConstantContext constantContext) {
        constants.add(getConstantFromContext(constantContext));
        return super.visitConstant(constantContext);
    }

    private int getConstantFromContext(SchemeParser.ConstantContext constantContext) {
        StringBuilder digitAsString = new StringBuilder();

        List<SchemeParser.DigitContext> digitContexts = constantContext.number().digit();
        for (SchemeParser.DigitContext context : digitContexts) {
            digitAsString.append(context.DIGIT().getSymbol().getText());

        }

        return Integer.valueOf(digitAsString.toString());
    }

}

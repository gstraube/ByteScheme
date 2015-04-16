import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.HashMap;
import java.util.Map;

public class ParserTestVisitor extends SchemeBaseVisitor<String> {

    private static final String QUOTATION_SYMBOL = "'";
    private static final String NO_VALUE = "";

    public Map<String, String> variableDefinitions = new HashMap<>();

    @Override
    public String visitExpression(SchemeParser.ExpressionContext expression) {
        return evaluateExpression(expression);
    }

    @Override
    public String visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        return processVariableDefinitions(variableDefinition);
    }

    @Override
    protected String defaultResult() {
        return NO_VALUE;
    }

    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        return aggregate + nextResult;
    }

    private String processVariableDefinitions(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();
        String value = evaluateExpression(variableDefinition.expression());
        variableDefinitions.put(identifier, value);
        return NO_VALUE;
    }

    private String evaluateExpression(SchemeParser.ExpressionContext expression) {
        if (expression.constant() != null) {
            return extractConstant(expression.constant());
        }
        if (expression.quotation() != null) {
            return applyQuotation(expression.quotation());
        }
        String identifier = expression.IDENTIFIER().getText();
        if (variableDefinitions.containsKey(identifier)) {
            return variableDefinitions.get(identifier);
        }
        throw new ParseCancellationException("Undefined variable");
    }

    private String applyQuotation(SchemeParser.QuotationContext quotation) {
        SchemeParser.DatumContext datum = quotation.datum();

        if (datum.constant() != null) {
            return extractConstant(datum.constant());
        }

        String identifier = datum.IDENTIFIER().getText();
        return QUOTATION_SYMBOL + identifier;
    }

    private String extractConstant(SchemeParser.ConstantContext constant) {
        if (constant.NUMBER() != null) {
            return constant.NUMBER().getText();
        }
        if (constant.CHARACTER() != null) {
            return constant.CHARACTER().getText();
        }
        if (constant.STRING() != null) {
            return constant.STRING().getText();
        }
        return constant.BOOLEAN().getText();
    }

}

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.HashMap;
import java.util.Map;

public class ParserTestVisitor extends SchemeBaseVisitor<String> {

    public Map<String, String> variableDefinitions = new HashMap<>();

    private static final String NO_VALUE = "";

    @Override
    public String visitExpression(SchemeParser.ExpressionContext expressionContext) {
        return evaluateExpression(expressionContext);
    }

    @Override
    public String visitVariable_definition(SchemeParser.Variable_definitionContext variable_definitionContext) {
        return processVariableDefinitions(variable_definitionContext);
    }

    @Override
    protected String defaultResult() {
        return NO_VALUE;
    }

    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        return aggregate + nextResult;
    }

    private String processVariableDefinitions(SchemeParser.Variable_definitionContext definitionContext) {
        String identifier = definitionContext.IDENTIFIER().getText();
        String value = evaluateExpression(definitionContext.expression());
        variableDefinitions.put(identifier, value);
        return NO_VALUE;
    }

    private String evaluateExpression(SchemeParser.ExpressionContext expression) {
        if (expression.constant() != null) {
            return getConstantFromContext(expression.constant());
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
        return getConstantFromContext(quotation.constant());
    }

    private String getConstantFromContext(SchemeParser.ConstantContext constantContext) {
        if (constantContext.NUMBER() != null) {
            return constantContext.NUMBER().getText();
        }
        if (constantContext.CHARACTER() != null) {
            return constantContext.CHARACTER().getText();
        }
        if (constantContext.STRING() != null) {
            return constantContext.STRING().getText();
        }
        return constantContext.BOOLEAN().getText();
    }

}

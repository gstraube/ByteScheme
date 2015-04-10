import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParserTestVisitor extends SchemeBaseVisitor<Void> {

    List<String> constants = new ArrayList<>();
    public Map<String, String> variableDefinitions = new HashMap<>();

    @Override
    public Void visitConstant(SchemeParser.ConstantContext constantContext) {
        constants.add(getConstantFromContext(constantContext));
        return super.visitConstant(constantContext);
    }

    @Override
    public Void visitVariable_definition(SchemeParser.Variable_definitionContext variable_definitionContext) {
        processVariableDefinitions(variable_definitionContext);
        return super.visitVariable_definition(variable_definitionContext);
    }

    private void processVariableDefinitions(SchemeParser.Variable_definitionContext definitionContext) {
        String identifier = definitionContext.IDENTIFIER().getText();
        String value = evaluateExpression(definitionContext.expression());
        variableDefinitions.put(identifier, value);
    }

    private String evaluateExpression(SchemeParser.ExpressionContext expression) {
        if (expression.constant() != null) {
            return getConstantFromContext(expression.constant());
        }
        String identifier = expression.IDENTIFIER().getText();
        if (variableDefinitions.containsKey(identifier)) {
            return variableDefinitions.get(identifier);
        }
        throw new RuntimeException("Undefined variable");
    }

    private String getConstantFromContext(SchemeParser.ConstantContext constantContext) {
        return constantContext.NUMBER().getText();
    }

}

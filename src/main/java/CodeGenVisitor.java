import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeGenVisitor extends SchemeBaseVisitor<List<String>> {

    private static final String INTEGER_CONSTANT_VAR_DEFINITION = "BigInteger %s = new BigInteger(%s);";
    private static final String CHAR_CONSTANT_VAR_DEFINITION = "char %s = '%c';";
    private static final String STRING_CONSTANT_VAR_DEFINITION = "String %s = %s;";
    private static final String BOOLEAN_CONSTANT_VAR_DEFINITION = "boolean %s = %s;";

    @Override
    public List<String> visitConstant(SchemeParser.ConstantContext constant) {
        String generatedCode = "";

        if (constant.NUMBER() != null) {
            generatedCode = String.format("new BigInteger(%s)", constant.NUMBER().getText());
        }
        if (constant.CHARACTER() != null) {
            char containedChar = constant.CHARACTER().getText().charAt(2);
            generatedCode = String.format("'%c'", containedChar);
        }
        if (constant.STRING() != null) {
            generatedCode = constant.STRING().getText();
        }
        if (constant.BOOLEAN() != null) {
            generatedCode = String.valueOf("#t".equals(constant.BOOLEAN().getText()));
        }

        return Collections.singletonList(generatedCode);
    }

    @Override
    public List<String> visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();

        String text;
        String generatedCode = "";
        SchemeParser.ConstantContext constant = variableDefinition.expression().constant();

        if (constant.NUMBER() != null) {
            text = constant.NUMBER().getText();
            generatedCode = String.format(INTEGER_CONSTANT_VAR_DEFINITION, identifier, text);
        }
        if (constant.CHARACTER() != null) {
            text = constant.CHARACTER().getText();
            char containedChar = text.charAt(2);
            generatedCode = String.format(CHAR_CONSTANT_VAR_DEFINITION, identifier, containedChar);
        }
        if (constant.STRING() != null) {
            text = constant.STRING().getText();
            generatedCode = String.format(STRING_CONSTANT_VAR_DEFINITION, identifier, text);
        }
        if (constant.BOOLEAN() != null) {
            text = constant.BOOLEAN().getText();
            boolean value = "#t".equals(text);
            generatedCode = String.format(BOOLEAN_CONSTANT_VAR_DEFINITION, identifier, value);
        }

        return Collections.singletonList(generatedCode);
    }

    @Override
    protected List<String> defaultResult() {
        return new ArrayList<>();
    }

    @Override
    protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
        aggregate.addAll(nextResult);
        return aggregate;
    }

}

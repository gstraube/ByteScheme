import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeGenVisitor extends SchemeBaseVisitor<List<String>> {

    @Override
    public List<String> visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();

        String text;
        String generatedCode = "";
        SchemeParser.ConstantContext constant = variableDefinition.expression().constant();

        if (constant.NUMBER() != null) {
            text = constant.NUMBER().getText();
            generatedCode = String.format("BigInteger %s = new BigInteger(%s);", identifier, text);
        }
        if (constant.CHARACTER() != null) {
            text = constant.CHARACTER().getText();
            char containedChar = text.charAt(2);
            generatedCode = String.format("char %s = '%c';", identifier, containedChar);
        }
        if (constant.STRING() != null) {
            text = constant.STRING().getText();
            generatedCode = String.format("String %s = %s;", identifier, text);
        }
        if (constant.BOOLEAN() != null) {
            text = constant.BOOLEAN().getText();
            boolean value = "#t".equals(text);
            generatedCode = String.format("boolean %s = %s;", identifier, value);
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

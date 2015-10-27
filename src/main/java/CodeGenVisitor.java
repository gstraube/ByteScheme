import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeGenVisitor extends SchemeBaseVisitor<List<String>> {

    @Override
    public List<String> visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();

        String text = variableDefinition.expression().constant().NUMBER().getText();

        String generatedCode = String.format("BigInteger %s = new BigInteger(%s)", identifier, text);
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

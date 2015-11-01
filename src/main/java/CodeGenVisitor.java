import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeGenVisitor extends SchemeBaseVisitor<List<String>> {

    private static final String INTEGER_CONSTANT_VAR_DEFINITION = "BigInteger %s = %s;";
    private static final String CHAR_CONSTANT_VAR_DEFINITION = "char %s = %s;";
    private static final String STRING_CONSTANT_VAR_DEFINITION = "String %s = %s;";
    private static final String BOOLEAN_CONSTANT_VAR_DEFINITION = "boolean %s = %s;";

    @Override
    public List<String> visitConstant(SchemeParser.ConstantContext constant) {
        String generatedCode = "";

        if (constant.NUMBER() != null) {
            generatedCode = String.format("new BigInteger(\"%s\")", constant.NUMBER().getText());
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
        SchemeParser.ConstantContext constant = variableDefinition.expression().constant();

        return Collections.singletonList(createVariableDefinitionForConstant(identifier, constant));
    }

    @Override
    public List<String> visitList(SchemeParser.ListContext list) {
        String sListClass = "class SList{";
        int elementCounter = 0;
        for (SchemeParser.DatumContext datum : list.datum()) {
            sListClass += createVariableDefinitionForConstant("e" + elementCounter, datum.constant());
            elementCounter++;
        }
        sListClass += "}";

        return Collections.singletonList(sListClass);
    }

    private String createVariableDefinitionForConstant(String identifier, SchemeParser.ConstantContext constant) {
        String text = visitConstant(constant).get(0);

        if (constant.NUMBER() != null) {
            return String.format(INTEGER_CONSTANT_VAR_DEFINITION, identifier, text);
        }
        if (constant.CHARACTER() != null) {
            return String.format(CHAR_CONSTANT_VAR_DEFINITION, identifier, text);
        }
        if (constant.STRING() != null) {
            return String.format(STRING_CONSTANT_VAR_DEFINITION, identifier, text);
        }
        if (constant.BOOLEAN() != null) {
            return String.format(BOOLEAN_CONSTANT_VAR_DEFINITION, identifier, text);
        }

        return "";
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

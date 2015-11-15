import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.concurrent.atomic.AtomicInteger;

public class CodeGenVisitor extends SchemeBaseVisitor<GeneratedCode> {

    private static final String INTEGER_CONSTANT_VAR_DEFINITION = "java.math.BigInteger %s = %s;";
    private static final String CHAR_CONSTANT_VAR_DEFINITION = "char %s = %s;";
    private static final String STRING_CONSTANT_VAR_DEFINITION = "String %s = %s;";
    private static final String BOOLEAN_CONSTANT_VAR_DEFINITION = "boolean %s = %s;";

    AtomicInteger constantsCounter = new AtomicInteger(0);

    @Override
    public GeneratedCode visitForm(SchemeParser.FormContext form) {
        GeneratedCode generatedCode = GeneratedCode.empty();


        if (form.expression() != null) {
            SchemeParser.ExpressionContext expression = form.expression();
            String constantCode = visitConstant(expression.constant()).getConstant(0);

            int constantIndex = constantsCounter.getAndIncrement();
            generatedCode.addMethodToBeDeclared(String.format("public String printConstant%d(){return String.valueOf(%s);}",
                    constantIndex, constantCode));
            generatedCode.addMethodToBeCalled("printConstant" + constantIndex);
        }
        if (form.definition() != null) {
            return visitVariable_definition(form.definition().variable_definition());
        }

        return generatedCode;
    }

    @Override
    public GeneratedCode visitConstant(SchemeParser.ConstantContext constant) {
        GeneratedCode generatedCode = GeneratedCode.empty();

        if (constant.NUMBER() != null) {
            generatedCode.addConstant(String.format("new java.math.BigInteger(\"%s\")", constant.NUMBER().getText()));
        }
        if (constant.CHARACTER() != null) {
            char containedChar;

            String characterText = constant.CHARACTER().getText().substring(2);
            if (characterText.length() > 1) {
                switch (characterText) {
                    case "newline":
                        containedChar = '\n';
                        break;
                    case "space":
                        containedChar = ' ';
                        break;
                    default:
                        throw new ParseCancellationException(
                                String.format("Could not evaluate character literal '%s'",
                                        constant.CHARACTER().getText()));
                }
            } else {
                containedChar = constant.CHARACTER().getText().charAt(2);
            }

            generatedCode.addConstant(String.format("'%c'", containedChar));
        }
        if (constant.STRING() != null) {
            generatedCode.addConstant(constant.STRING().getText());
        }
        if (constant.BOOLEAN() != null) {
            generatedCode.addConstant(String.valueOf("#t".equals(constant.BOOLEAN().getText())));
        }

        return generatedCode;
    }


    @Override
    public GeneratedCode visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();
        SchemeParser.ConstantContext constant = variableDefinition.expression().constant();

        return createVariableDefinitionForConstant(identifier, constant);
    }

    private GeneratedCode createVariableDefinitionForConstant(String identifier,
                                                              SchemeParser.ConstantContext constant) {
        GeneratedCode generatedCode = GeneratedCode.empty();
        String text = visitConstant(constant).getConstant(0);

        if (constant.NUMBER() != null) {
            generatedCode.addVariableDefinition(String.format(INTEGER_CONSTANT_VAR_DEFINITION, identifier, text));
        }
        if (constant.CHARACTER() != null) {
            generatedCode.addVariableDefinition(String.format(CHAR_CONSTANT_VAR_DEFINITION, identifier, text));
        }
        if (constant.STRING() != null) {
            generatedCode.addVariableDefinition(String.format(STRING_CONSTANT_VAR_DEFINITION, identifier, text));
        }
        if (constant.BOOLEAN() != null) {
            generatedCode.addVariableDefinition(String.format(BOOLEAN_CONSTANT_VAR_DEFINITION, identifier, text));
        }

        return generatedCode;
    }

    @Override
    protected GeneratedCode defaultResult() {
        return GeneratedCode.empty();
    }

    @Override
    protected GeneratedCode aggregateResult(GeneratedCode aggregate, GeneratedCode nextResult) {
        return aggregate.mergeWith(nextResult);
    }

}

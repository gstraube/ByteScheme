import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CodeGenVisitor extends SchemeBaseVisitor<GeneratedCode> {

    private Map<String, VariableDefinition> identifierToVariableDefinition = new HashMap<>();

    AtomicInteger constantsCounter = new AtomicInteger(0);

    @Override
    public GeneratedCode visitForm(SchemeParser.FormContext form) {
        GeneratedCode generatedCode = GeneratedCode.empty();

        if (form.expression() != null) {
            if (form.expression().constant() != null) {
                SchemeParser.ExpressionContext expression = form.expression();
                String constantCode = visitConstant(expression.constant()).getConstant(0);

                int constantIndex = constantsCounter.getAndIncrement();
                generatedCode.addMethodToBeDeclared(String.format("public String printConstant%d(){return String.valueOf(%s);}",
                        constantIndex, constantCode));
                generatedCode.addMethodToBeCalled("printConstant" + constantIndex);

            }
            TerminalNode identifier = form.expression().IDENTIFIER();
            if (identifier != null) {
                String identifierText = identifier.getText();
                generatedCode.addMethodToBeDeclared(String.format("public String %s(){return String.valueOf(%s);}",
                        identifierText, identifierText));
                generatedCode.addMethodToBeCalled(identifierText);
            }
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
        GeneratedCode generatedCode = GeneratedCode.empty();
        String identifier = variableDefinition.IDENTIFIER().getText();

        SchemeParser.ExpressionContext expression = variableDefinition.expression();
        if (expression.constant() != null) {
            VariableDefinition variableDefinitionForConstant = createVariableDefinitionForConstant(identifier,
                    expression.constant());
            identifierToVariableDefinition.put(identifier, variableDefinitionForConstant);
            generatedCode.addVariableDefinition(variableDefinitionForConstant.toString());

            return generatedCode;
        }
        if (expression.IDENTIFIER() != null) {
            String referencedVariableIdentifier = expression.IDENTIFIER().getText();
            VariableDefinition referencedVariableDefinition =
                    identifierToVariableDefinition.get(referencedVariableIdentifier);

            VariableDefinition definition = referencedVariableDefinition.referencedBy(identifier);

            generatedCode.addVariableDefinition(definition.toString());

            return generatedCode;
        }

        return GeneratedCode.empty();
    }

    private VariableDefinition createVariableDefinitionForConstant(String identifier,
                                                              SchemeParser.ConstantContext constant) {
        String text = visitConstant(constant).getConstant(0);

        if (constant.NUMBER() != null) {
            return VariableDefinition.createForBigInteger(identifier, text);
        }
        if (constant.CHARACTER() != null) {
            return VariableDefinition.createForChar(identifier, text);
        }
        if (constant.STRING() != null) {
            return VariableDefinition.createForString(identifier, text);
        }

        return VariableDefinition.createForBoolean(identifier, text);
    }

    @Override
    protected GeneratedCode defaultResult() {
        return GeneratedCode.empty();
    }

    @Override
    protected GeneratedCode aggregateResult(GeneratedCode aggregate, GeneratedCode nextResult) {
        return aggregate.mergeWith(nextResult);
    }

    private static class VariableDefinition {

        private static final String INTEGER_CONSTANT_VAR_DEFINITION = "java.math.BigInteger %s = %s;";
        private static final String CHAR_CONSTANT_VAR_DEFINITION = "char %s = %s;";
        private static final String STRING_CONSTANT_VAR_DEFINITION = "String %s = %s;";
        private static final String BOOLEAN_CONSTANT_VAR_DEFINITION = "boolean %s = %s;";

        private VariableType type;
        private String identifier;
        private String value;

        public static VariableDefinition createForBigInteger(String identifier, String value) {
            return new VariableDefinition(VariableType.BIG_INTEGER, identifier, value);
        }

        public static VariableDefinition createForChar(String identifier, String value) {
            return new VariableDefinition(VariableType.CHAR, identifier, value);
        }

        public static VariableDefinition createForString(String identifier, String value) {
            return new VariableDefinition(VariableType.STRING, identifier, value);
        }

        public static VariableDefinition createForBoolean(String identifier, String value) {
            return new VariableDefinition(VariableType.BOOLEAN, identifier, value);
        }

        @Override
        public String toString() {
            String template;

            switch (type) {
                case BIG_INTEGER:
                    template = INTEGER_CONSTANT_VAR_DEFINITION;
                    break;
                case CHAR:
                    template = CHAR_CONSTANT_VAR_DEFINITION;
                    break;
                case STRING:
                    template = STRING_CONSTANT_VAR_DEFINITION;
                    break;
                case BOOLEAN:
                    template = BOOLEAN_CONSTANT_VAR_DEFINITION;
                    break;
                default:
                    throw new ParseCancellationException("Unknown type in variable definition");
            }

            return String.format(template, identifier, value);
        }

        private VariableDefinition(VariableType type, String identifier, String value) {
            this.type = type;
            this.identifier = identifier;
            this.value = value;
        }

        public VariableDefinition referencedBy(String identifier) {
            this.value = this.identifier;
            this.identifier = identifier;
            return this;
        }

        private enum VariableType {
            BIG_INTEGER, CHAR, STRING, BOOLEAN
        }
    }

}

import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.HashMap;
import java.util.Map;

public class CodeGenVisitor extends SchemeBaseVisitor<GeneratedCode> {

    private static final String UNDEFINED_VARIABLE_EXCEPTION_MESSAGE = "Undefined variable '%s'";
    private Map<String, VariableDefinition> identifierToVariableDefinition = new HashMap<>();

    @Override
    public GeneratedCode visitConstant(SchemeParser.ConstantContext constant) {
        GeneratedCode generatedCode = GeneratedCode.empty();

        String constantCode = "";
        if (constant.NUMBER() != null) {
            constantCode = String.format("new java.math.BigInteger(\"%s\");", constant.NUMBER().getText());
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

            String formattedString = String.format("new Character('%c');", containedChar);
            constantCode = formattedString.replace("\n", "\\n");
        }
        if (constant.STRING() != null) {
            constantCode = String.format("new String(%s);", constant.STRING().getText());
        }
        if (constant.BOOLEAN() != null) {
            constantCode = String.format("new Boolean(%b);", "#t".equals(constant.BOOLEAN().getText()));
        }

        generatedCode.addConstant(constantCode);

        return generatedCode;
    }

    @Override
    public GeneratedCode visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        GeneratedCode generatedCode = GeneratedCode.empty();

        String identifier = variableDefinition.IDENTIFIER().getText();
        String variableCode = "";

        SchemeParser.ExpressionContext expression = variableDefinition.expression();
        if (expression.constant() != null) {
            VariableDefinition variableDefinitionForConstant = createVariableDefinitionForConstant(identifier,
                    expression.constant());
            identifierToVariableDefinition.put(identifier, variableDefinitionForConstant);


            variableCode = variableDefinitionForConstant.toString();
        }
        if (expression.IDENTIFIER() != null) {
            String referencedVariableIdentifier = expression.IDENTIFIER().getText();

            if (!identifierToVariableDefinition.containsKey(referencedVariableIdentifier)) {
                throw new ParseCancellationException(String.format(UNDEFINED_VARIABLE_EXCEPTION_MESSAGE,
                        referencedVariableIdentifier));
            }

            VariableDefinition referencedVariableDefinition =
                    identifierToVariableDefinition.get(referencedVariableIdentifier);

            VariableDefinition definition = referencedVariableDefinition.referencedBy(identifier);

            variableCode = definition.toString();
        }

        generatedCode.addVariableDefinition(variableCode);

        return generatedCode;
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

        private static final String INTEGER_CONSTANT_VAR_DEFINITION = "java.math.BigInteger %s = %s";
        private static final String CHAR_CONSTANT_VAR_DEFINITION = "char %s = %s";
        private static final String STRING_CONSTANT_VAR_DEFINITION = "String %s = %s";
        private static final String BOOLEAN_CONSTANT_VAR_DEFINITION = "boolean %s = %s";

        private VariableType type;
        private String identifier;
        private String value;
        private boolean needsSemicolon = false;

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

            String definitionStatement = String.format(template, identifier, value);
            return needsSemicolon ? definitionStatement.concat(";") : definitionStatement;
        }

        private VariableDefinition(VariableType type, String identifier, String value) {
            this.type = type;
            this.identifier = identifier;
            this.value = value;
        }

        public VariableDefinition referencedBy(String identifier) {
            this.value = this.identifier;
            this.identifier = identifier;
            needsSemicolon = true;
            return this;
        }

        private enum VariableType {
            BIG_INTEGER, CHAR, STRING, BOOLEAN
        }
    }

}

import org.antlr.v4.runtime.misc.ParseCancellationException;

public class VariableDefinition {

    private static final String INTEGER_CONSTANT_VAR_DEFINITION = "static BigInteger %s = %s";
    private static final String CHAR_CONSTANT_VAR_DEFINITION = "static Character %s = %s";
    private static final String STRING_CONSTANT_VAR_DEFINITION = "static String %s = %s";
    private static final String BOOLEAN_CONSTANT_VAR_DEFINITION = "static Boolean %s = %s";

    private VariableType type;
    private String identifier;
    private String value;
    private boolean needsSemicolon = false;

    private VariableDefinition(VariableType type, String identifier, String value) {
        this.type = type;
        this.identifier = identifier;
        this.value = value;
    }

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

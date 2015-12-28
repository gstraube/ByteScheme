import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CodeGenVisitor extends SchemeBaseVisitor<GeneratedCode.GeneratedCodeBuilder> {

    private static final String UNDEFINED_VARIABLE_EXCEPTION_MESSAGE = "Undefined variable '%s'";

    private static final String DISPLAY_PROCEDURE_NAME = "display";
    private static final String LIST_PROCEDURE_NAME = "list";
    private static final String CAR_PROCEDURE_NAME = "car";
    private static final String CDR_PROCEDURE_NAME = "cdr";
    private static final int LESS_THAN = -1;
    private static final int EQUAL = 0;
    private static final int GREATER_THAN = 1;

    private final Map<String, CodeGenProcedure> procedureMap = new HashMap<>();

    private final Function<SchemeParser.ExpressionContext, String> expressionToCode = expression -> {
        if (expression.constant() != null) {
            String constant = visitConstant(expression.constant()).getConstant(0);
            return constant.substring(0, constant.length() - 1);
        }

        if (expression.IDENTIFIER() != null) {
            return expression.IDENTIFIER().getText();
        }

        if (expression.application() != null) {
            return visitApplication(expression.application()).getConstant(0);
        }

        return "";
    };

    private Map<String, VariableDefinition> identifierToVariableDefinition = new HashMap<>();

    public CodeGenVisitor() {
        procedureMap.put(DISPLAY_PROCEDURE_NAME, expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 1) {
                SchemeParser.ExpressionContext argument = expressions.get(0);

                String mainMethodStatement = "System.out.println(OutputFormatter.output(%s));";
                codeBuilder.addStatementsToMainMethod(String.format(mainMethodStatement,
                        expressionToCode.apply(argument)));
            }

            return codeBuilder;
        });

        procedureMap.put(LIST_PROCEDURE_NAME, expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            String listCode;
            if (expressions.size() == 0) {
                listCode = "ListWrapper.fromElements(new Object[0])";
            } else {
                String listArguments = expressions.stream()
                        .map(expressionToCode)
                        .collect(Collectors.joining(","));
                listCode = String.format("ListWrapper.fromElements(new Object[]{%s})", listArguments);
            }
            codeBuilder.addConstant(listCode);

            return codeBuilder;
        });

        procedureMap.put(CAR_PROCEDURE_NAME, createListProcedure("car"));
        procedureMap.put(CDR_PROCEDURE_NAME, createListProcedure("cdr"));

        procedureMap.put("+", createChainedProcedure("add"));
        procedureMap.put("-", createChainedProcedure("subtract", "negate"));
        procedureMap.put("*", createChainedProcedure("multiply"));
        procedureMap.put("quotient", createChainedProcedure("divide"));
        procedureMap.put("<", createComparisonProcedure(LESS_THAN));
        procedureMap.put("<=", createComparisonProcedure(LESS_THAN, EQUAL));
        procedureMap.put(">", createComparisonProcedure(GREATER_THAN));
        procedureMap.put(">=", createComparisonProcedure(GREATER_THAN, EQUAL));

        procedureMap.put("equal?", expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 2) {
                String firstArgument = expressionToCode.apply(expressions.get(0));
                String secondArgument = expressionToCode.apply(expressions.get(1));

                codeBuilder.addConstant(String.format("new Boolean(java.util.Objects.equals(%s,%s))", firstArgument, secondArgument));
            }

            return codeBuilder;
        });
    }

    private CodeGenProcedure createListProcedure(String procedureName) {
        return expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 1) {
                if (expressions.get(0).application() != null) {
                    String list = visitApplication(expressions.get(0).application()).getConstant(0);

                    codeBuilder.addConstant(String.format("%s.%s()", list, procedureName));
                }
            }

            return codeBuilder;
        };
    }

    private CodeGenProcedure createComparisonProcedure(Integer... expectedResults) {
        return expressions -> {
            GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();
            List<Integer> results = Arrays.asList(expectedResults);
            List<String> comparisons = new ArrayList<>();

            for (int current = 0; current < expressions.size() - 1; current++) {
                int next = current + 1;
                String compareTo = String.format("%s.compareTo(%s)", expressionToCode.apply(expressions.get(current)),
                        expressionToCode.apply(expressions.get(next)));

                String comparison = results
                        .stream()
                        .map(expectedResult -> String.format("%s == %d", compareTo, expectedResult))
                        .collect(Collectors.joining("||", "(", ")"));

                comparisons.add(comparison);
            }

            String completeComparison = comparisons
                    .stream()
                    .collect(Collectors.joining("&&", "new Boolean(", ")"));

            generatedCodeBuilder.addConstant(completeComparison);

            return generatedCodeBuilder;
        };
    }

    private CodeGenProcedure createChainedProcedure(String procedureName, String singleArgumentProcedure) {
        return expressions -> {
            if (expressions.size() == 1) {
                GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();
                generatedCodeBuilder.addConstant(String.format("%s.%s()",
                        expressionToCode.apply(expressions.get(0)), singleArgumentProcedure));

                return generatedCodeBuilder;
            }

            return chainMethodCalls(procedureName, expressions);
        };
    }

    private CodeGenProcedure createChainedProcedure(String procedureName) {
        return expressions -> chainMethodCalls(procedureName, expressions);
    }

    private GeneratedCode.GeneratedCodeBuilder chainMethodCalls(String procedureName,
                                                                List<SchemeParser.ExpressionContext> expressions) {
        GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();

        String constant = expressionToCode.apply(expressions.get(0));
        for (int current = 1; current < expressions.size(); current++) {
            constant += String.format(".%s(%s)", procedureName, expressionToCode.apply(expressions.get(current)));
        }

        generatedCodeBuilder.addConstant(constant);

        return generatedCodeBuilder;
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitApplication(SchemeParser.ApplicationContext application) {
        String identifier = application.IDENTIFIER().getText();

        if (procedureMap.containsKey(identifier)) {
            CodeGenProcedure codeGenProcedure = procedureMap.get(identifier);

            return codeGenProcedure.generateCode(application.expression());
        }

        return new GeneratedCode.GeneratedCodeBuilder();
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitConstant(SchemeParser.ConstantContext constant) {
        GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

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

        codeBuilder.addConstant(constantCode);

        return codeBuilder;
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        GeneratedCode.GeneratedCodeBuilder generatedCode = new GeneratedCode.GeneratedCodeBuilder();

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

        return generatedCode.addVariableDefinition(variableCode);
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
    protected GeneratedCode.GeneratedCodeBuilder defaultResult() {
        return new GeneratedCode.GeneratedCodeBuilder();
    }

    @Override
    protected GeneratedCode.GeneratedCodeBuilder aggregateResult(GeneratedCode.GeneratedCodeBuilder aggregate,
                                                                 GeneratedCode.GeneratedCodeBuilder nextResult) {
        return aggregate.mergeWith(nextResult);
    }

    private static class VariableDefinition {

        private static final String INTEGER_CONSTANT_VAR_DEFINITION = "static java.math.BigInteger %s = %s";
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

    interface CodeGenProcedure {
        GeneratedCode.GeneratedCodeBuilder generateCode(List<SchemeParser.ExpressionContext> expression);
    }

}

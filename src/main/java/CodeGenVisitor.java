import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    private static AtomicInteger methodIndex = new AtomicInteger(0);

    private final Map<String, CodeGenProcedure> procedureMap = new HashMap<>();

    private final Function<SchemeParser.ExpressionContext, GeneratedCode.GeneratedCodeBuilder> expressionToCode = expression -> {
        GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();
        String codeConstant = "";

        if (expression.constant() != null) {
            String constant = visitConstant(expression.constant()).getGeneratedCode();
            codeConstant = constant.substring(0, constant.length() - 1);
        }

        if (expression.IDENTIFIER() != null) {
            codeConstant = expression.IDENTIFIER().getText();
        }

        if (expression.application() != null) {
            GeneratedCode.GeneratedCodeBuilder genCodeBuilder = visitApplication(expression.application());
            codeConstant = genCodeBuilder.getGeneratedCode();
            codeBuilder = codeBuilder.mergeWith(genCodeBuilder);
        }

        codeBuilder.setGeneratedCode(codeConstant);

        return codeBuilder;
    };

    private Map<String, VariableDefinition> identifierToVariableDefinition = new HashMap<>();

    public CodeGenVisitor() {
        procedureMap.put(DISPLAY_PROCEDURE_NAME, expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 1) {
                SchemeParser.ExpressionContext argument = expressions.get(0);

                String mainMethodStatement = "System.out.println(OutputFormatter.output(%s));";
                GeneratedCode.GeneratedCodeBuilder genCodeBuilder = expressionToCode.apply(argument);
                codeBuilder.addStatementsToMainMethod(String.format(mainMethodStatement,
                        genCodeBuilder.getGeneratedCode()));

                codeBuilder = codeBuilder.mergeWith(genCodeBuilder);
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
                        .map(GeneratedCode.GeneratedCodeBuilder::getGeneratedCode)
                        .collect(Collectors.joining(","));
                listCode = String.format("ListWrapper.fromElements(new Object[]{%s})", listArguments);
            }
            codeBuilder.setGeneratedCode(listCode);

            return codeBuilder;
        });

        procedureMap.put(CAR_PROCEDURE_NAME, createListProcedure("car"));
        procedureMap.put(CDR_PROCEDURE_NAME, createListProcedure("cdr"));

        procedureMap.put("+", createProcedure("PredefinedProcedures.add", "%s(new Object[]{%s})"));
        procedureMap.put("-", createChainedProcedure("PredefinedProcedures.subtract", "PredefinedProcedures.negate"));
        procedureMap.put("*", createProcedure("PredefinedProcedures.multiply", "%s(new Object[]{%s})"));
        procedureMap.put("quotient", createProcedure("PredefinedProcedures.divide", "%s(new Object[]{%s})"));
        procedureMap.put("<", createComparisonProcedure(LESS_THAN));
        procedureMap.put("<=", createComparisonProcedure(LESS_THAN, EQUAL));
        procedureMap.put(">", createComparisonProcedure(GREATER_THAN));
        procedureMap.put(">=", createComparisonProcedure(GREATER_THAN, EQUAL));

        procedureMap.put("equal?", expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 2) {
                String firstArgument = expressionToCode.apply(expressions.get(0)).getGeneratedCode();
                String secondArgument = expressionToCode.apply(expressions.get(1)).getGeneratedCode();

                codeBuilder.setGeneratedCode(String.format("java.util.Objects.equals(%s,%s)", firstArgument, secondArgument));
            }

            return codeBuilder;
        });
    }

    private CodeGenProcedure createListProcedure(String procedureName) {
        return expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 1) {
                if (expressions.get(0).application() != null) {
                    String list = visitApplication(expressions.get(0).application()).getGeneratedCode();

                    codeBuilder.setGeneratedCode(String.format("%s.%s()", list, procedureName));
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
                String compareTo = String.format("((BigInteger) %s).compareTo(%s)",
                        expressionToCode.apply(expressions.get(current)).getGeneratedCode(),
                        expressionToCode.apply(expressions.get(next)).getGeneratedCode());

                String comparison = results
                        .stream()
                        .map(expectedResult -> String.format("%s == %d", compareTo, expectedResult))
                        .collect(Collectors.joining("||"));

                comparisons.add(comparison);
            }

            String completeComparison = comparisons
                    .stream()
                    .collect(Collectors.joining("&&", "(", ")"));

            generatedCodeBuilder.setGeneratedCode(completeComparison);

            return generatedCodeBuilder;
        };
    }

    private CodeGenProcedure createChainedProcedure(String procedureName, String singleArgumentProcedure) {
        return expressions -> {
            if (expressions.size() == 1) {
                return createProcedure(singleArgumentProcedure, "%s(new Object[]{%s})").generateCode(expressions);
            } else {
                return createProcedure(procedureName, "%s(new Object[]{%s})").generateCode(expressions);
            }
        };
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitApplication(SchemeParser.ApplicationContext application) {
        String identifier = application.IDENTIFIER().getText();

        List<SchemeParser.ExpressionContext> expressions = application.expression();
        if ("if".equalsIgnoreCase(identifier)) {
            if (expressions.size() == 3) {
                GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();
                String methodName = String.format("evaluateIf%d()", methodIndex.getAndIncrement());

                String ifStatement =
                        String.format("public static Object %s {if(%s){return %s;}else{return %s;}}",
                                methodName,
                                expressionToCode.apply(expressions.get(0)).getGeneratedCode(),
                                expressionToCode.apply(expressions.get(1)).getGeneratedCode(),
                                expressionToCode.apply(expressions.get(2)).getGeneratedCode());

                codeBuilder.addMethodsToBeDeclared(ifStatement);
                codeBuilder.setGeneratedCode(methodName);

                return codeBuilder;
            }
        } else if (procedureMap.containsKey(identifier)) {
            CodeGenProcedure codeGenProcedure = procedureMap.get(identifier);

            return codeGenProcedure.generateCode(expressions);
        } else {
            return createProcedure(identifier, "%s(%s)").generateCode(expressions);
        }

        return new GeneratedCode.GeneratedCodeBuilder();
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitConstant(SchemeParser.ConstantContext constant) {
        GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

        String constantCode = "";
        if (constant.NUMBER() != null) {
            constantCode = String.format("new BigInteger(\"%s\");", constant.NUMBER().getText());
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

        codeBuilder.setGeneratedCode(constantCode);

        return codeBuilder;
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitDefinition(SchemeParser.DefinitionContext definition) {
        return definition.variable_definition() != null ? visitVariable_definition(definition.variable_definition()) :
                visitProcedure_definition(definition.procedure_definition());
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

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitProcedure_definition(SchemeParser.Procedure_definitionContext
                                                                                procedureDefinition) {
        GeneratedCode.GeneratedCodeBuilder codeBuilder = procedureDefinition.definition()
                .stream()
                .map(this::visitDefinition)
                .reduce(new GeneratedCode.GeneratedCodeBuilder(), GeneratedCode.GeneratedCodeBuilder::mergeWith);

        String procedureName = procedureDefinition.proc_name().IDENTIFIER().getText();
        List<SchemeParser.ExpressionContext> expression = procedureDefinition.expression();

        procedureMap.put(procedureName, createProcedure(procedureName, "%s(%s)"));

        SchemeParser.ExpressionContext lastExpression = expression.get(expression.size() - 1);

        String generatedMethod = "";
        if (lastExpression.constant() != null) {
            generatedMethod = String.format("public static Object %s(){return %s;}", procedureName,
                    expressionToCode.apply(lastExpression).getGeneratedCode());
        }
        SchemeParser.ApplicationContext application = lastExpression.application();
        if (application != null) {
            String params = procedureDefinition
                    .param()
                    .stream()
                    .map(p -> "Object " + p.IDENTIFIER().getText())
                    .collect(Collectors.joining(","));

            String body;
            if ("if".equalsIgnoreCase(application.IDENTIFIER().getText())) {
                List<SchemeParser.ExpressionContext> expressions = application.expression();
                body = String.format("if(%s){return %s;}else{return %s;}",
                        expressionToCode.apply(expressions.get(0)).getGeneratedCode(),
                        expressionToCode.apply(expressions.get(1)).getGeneratedCode(),
                        expressionToCode.apply(expressions.get(2)).getGeneratedCode());
            } else {
                body = "return " + visitApplication(application).getGeneratedCode() + ";";
            }

            generatedMethod = String.format("public static Object %s(%s){%s}",
                    procedureName, params, body);
        }
        if (lastExpression.IDENTIFIER() != null) {
            generatedMethod = String.format("public static Object %s(){return %s;}", procedureName,
                    expressionToCode.apply(lastExpression).getGeneratedCode());
        }

        codeBuilder.addMethodsToBeDeclared(generatedMethod);

        return codeBuilder;
    }

    private CodeGenProcedure createProcedure(String procedureName, String template) {
        return expressions -> {
            GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();
            String arguments = expressions.stream()
                    .map(expressionToCode)
                    .map(GeneratedCode.GeneratedCodeBuilder::getGeneratedCode)
                    .collect(Collectors.joining(","));
            generatedCodeBuilder.setGeneratedCode(String.format(template, procedureName, arguments));

            return generatedCodeBuilder;
        };
    }

    private VariableDefinition createVariableDefinitionForConstant(String identifier,
                                                                   SchemeParser.ConstantContext constant) {
        String text = visitConstant(constant).getGeneratedCode();

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

    interface CodeGenProcedure {
        GeneratedCode.GeneratedCodeBuilder generateCode(List<SchemeParser.ExpressionContext> expression);
    }

}

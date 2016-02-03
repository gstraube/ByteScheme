import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CodeGenVisitor extends SchemeBaseVisitor<GeneratedCode.GeneratedCodeBuilder> {

    private static final String UNDEFINED_VARIABLE_EXCEPTION_MESSAGE = "Undefined variable '%s'";

    private static AtomicInteger methodIndex = new AtomicInteger(0);

    private final Map<String, CodeGenProcedure> procedureMap;

    private Map<String, String> substitutions = new HashMap<>();

    public Function<SchemeParser.ExpressionContext, GeneratedCode.GeneratedCodeBuilder> expressionToCode() {
        return expression -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();
            String codeConstant = "";

            if (expression.constant() != null) {
                String constant = visitConstant(expression.constant()).getGeneratedCode();
                codeConstant = constant.substring(0, constant.length() - 1);
            }

            if (expression.IDENTIFIER() != null) {
                codeConstant = getIdentifierText(expression.IDENTIFIER());
            }

            if (expression.application() != null) {
                GeneratedCode.GeneratedCodeBuilder genCodeBuilder = visitApplication(expression.application());
                codeConstant = genCodeBuilder.getGeneratedCode();
                codeBuilder = codeBuilder.mergeWith(genCodeBuilder);
            }

            codeBuilder.setGeneratedCode(codeConstant);

            return codeBuilder;
        };
    }

    private Map<String, VariableDefinition> identifierToVariableDefinition = new HashMap<>();

    public CodeGenVisitor() {
        ProcedureMapInitializer procedureMapInitializer = new ProcedureMapInitializer(this);
        procedureMap = procedureMapInitializer.getInitialMap();
    }

    @Override
    public GeneratedCode.GeneratedCodeBuilder visitApplication(SchemeParser.ApplicationContext application) {
        String identifier = getIdentifierText(application.IDENTIFIER());

        List<SchemeParser.ExpressionContext> expressions = application.expression();
        if ("if".equalsIgnoreCase(identifier)) {
            if (expressions.size() == 3) {
                GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();
                String methodName = String.format("evaluateIf%d()", methodIndex.getAndIncrement());

                String ifStatement =
                        String.format("public static Object %s {if(%s){return %s;}else{return %s;}}",
                                methodName,
                                expressionToCode().apply(expressions.get(0)).getGeneratedCode(),
                                expressionToCode().apply(expressions.get(1)).getGeneratedCode(),
                                expressionToCode().apply(expressions.get(2)).getGeneratedCode());

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

        String identifier = getIdentifierText(variableDefinition.IDENTIFIER());
        String variableCode = "";

        SchemeParser.ExpressionContext expression = variableDefinition.expression();
        if (expression.constant() != null) {
            VariableDefinition variableDefinitionForConstant = createVariableDefinitionForConstant(identifier,
                    expression.constant());
            identifierToVariableDefinition.put(identifier, variableDefinitionForConstant);


            variableCode = variableDefinitionForConstant.toString();
        }
        if (expression.IDENTIFIER() != null) {
            String referencedVariableIdentifier = getIdentifierText(expression.IDENTIFIER());

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

        String procedureName = getIdentifierText(procedureDefinition.proc_name().IDENTIFIER());
        List<SchemeParser.ExpressionContext> expression = procedureDefinition.expression();

        procedureMap.put(procedureName, createProcedure(procedureName, "%s(%s)"));

        SchemeParser.ExpressionContext lastExpression = expression.get(expression.size() - 1);

        String generatedMethod = "";
        if (lastExpression.constant() != null) {
            generatedMethod = String.format("public static Object %s(){return %s;}", procedureName,
                    expressionToCode().apply(lastExpression).getGeneratedCode());
        }
        SchemeParser.ApplicationContext application = lastExpression.application();
        if (application != null) {
            String params = procedureDefinition
                    .param()
                    .stream()
                    .map(p -> "Object " + getIdentifierText(p.IDENTIFIER()))
                    .collect(Collectors.joining(","));

            String body = constructProcedureBody(procedureDefinition, procedureName, application);

            generatedMethod = String.format("public static Object %s(%s){%s}",
                    procedureName, params, body);
        }
        if (lastExpression.IDENTIFIER() != null) {
            generatedMethod = String.format("public static Object %s(){return %s;}", procedureName,
                    expressionToCode().apply(lastExpression).getGeneratedCode());
        }

        codeBuilder.addMethodsToBeDeclared(generatedMethod);

        return codeBuilder;
    }

    private String constructProcedureBody(SchemeParser.Procedure_definitionContext procedureDefinition, String procedureName, SchemeParser.ApplicationContext application) {
        String body;
        if ("if".equalsIgnoreCase(getIdentifierText(application.IDENTIFIER()))) {
            Optional<String> optimizedTailRecursion = optimizeTailRecursion(application, procedureName,
                    procedureDefinition.param());

            if (optimizedTailRecursion.isPresent()) {
                body = optimizedTailRecursion.get();
            } else {
                List<SchemeParser.ExpressionContext> expressions = application.expression();
                body = String.format("if(%s){return %s;}else{return %s;}",
                        expressionToCode().apply(expressions.get(0)).getGeneratedCode(),
                        expressionToCode().apply(expressions.get(1)).getGeneratedCode(),
                        expressionToCode().apply(expressions.get(2)).getGeneratedCode());
            }
        } else {
            body = "return " + visitApplication(application).getGeneratedCode() + ";";
        }
        return body;
    }

    public CodeGenProcedure createProcedure(String procedureName, String template) {
        return expressions -> {
            GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();
            String arguments = expressions.stream()
                    .map(expressionToCode())
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

    public Optional<String> optimizeTailRecursion(SchemeParser.ApplicationContext application, String procedureName,
                                                  List<SchemeParser.ParamContext> param) {
        List<SchemeParser.ExpressionContext> expressions = application.expression();

        SchemeParser.ExpressionContext conditionalExpression = expressions.get(0);

        Optional<ProcedureStructure> procedureStructure = constructProcedureStructure(procedureName, expressions);
        if (!procedureStructure.isPresent()) {
            return Optional.empty();
        }

        String returnStatement = "return "
                + expressionToCode().apply(procedureStructure.get().returnExpression).getGeneratedCode() + ";";

        List<String> paramNames = getParamNames(param);
        List<String> tailCallExpressions = getTailCallExpressions(procedureStructure.get().tailCall, paramNames);

        String arrayInitialization = "Object[] vars={" + String.join(",", paramNames) + "};";

        if (paramNames.size() != tailCallExpressions.size()) {
            return Optional.empty();
        }

        String whileLoop = createWhileLoop(conditionalExpression, procedureStructure.get().negateCondition,
                paramNames, tailCallExpressions);

        return Optional.of(arrayInitialization + whileLoop + returnStatement);
    }

    private Optional<ProcedureStructure> constructProcedureStructure(String procedureName, List<SchemeParser.ExpressionContext> expressions) {
        SchemeParser.ExpressionContext expression1 = expressions.get(1);
        SchemeParser.ExpressionContext expression2 = expressions.get(2);

        boolean isFirstExpressionTailCall = Objects.nonNull(expression1.application())
                && Objects.equals(procedureName, getIdentifierText(expression1.application().IDENTIFIER()));
        boolean isSecondExpressionTailCall = Objects.nonNull(expression2.application())
                && Objects.equals(procedureName, getIdentifierText(expression2.application().IDENTIFIER()));

        if (isFirstExpressionTailCall) {
            return Optional.of(new ProcedureStructure(false, expression1.application(), expression2));
        } else if (isSecondExpressionTailCall) {
            return Optional.of(new ProcedureStructure(true, expression2.application(), expression1));
        }

        return Optional.empty();
    }

    private String createWhileLoop(SchemeParser.ExpressionContext conditionalExpression, boolean negateCondition,
                                   List<String> paramNames, List<String> tailCallExpressions) {
        String assignments = createAssignments("%s=%s;", paramNames, tailCallExpressions);
        for (String paramName : paramNames) {
            assignments += String.format("vars[%d]=%s;", paramNames.indexOf(paramName), paramName);
        }
        String condition = expressionToCode().apply(conditionalExpression).getGeneratedCode();

        String whileTemplate = negateCondition ? "while(!%s){%s}" : "while(%s){%s}";
        return String.format(whileTemplate,
                condition,
                assignments);
    }

    private List<String> getParamNames(List<SchemeParser.ParamContext> param) {
        return param
                .stream()
                .map(p -> getIdentifierText(p.IDENTIFIER()))
                .collect(Collectors.toList());
    }

    private List<String> getTailCallExpressions(SchemeParser.ApplicationContext tailCall, List<String> paramNames) {
        for (String paramName : paramNames) {
            substitutions.put(paramName, String.format("vars[%d]", paramNames.indexOf(paramName)));
        }
        List<String> tailCallExpressions = tailCall.expression()
                .stream()
                .map(expressionToCode())
                .map(GeneratedCode.GeneratedCodeBuilder::getGeneratedCode)
                .collect(Collectors.toList());
        substitutions.clear();
        return tailCallExpressions;
    }

    private String getIdentifierText(TerminalNode identifier) {
        String identifierText = identifier.getText();
        return substitutions.containsKey(identifierText) ? substitutions.get(identifierText) : identifierText;
    }

    private String createAssignments(String template, List<String> paramDeclarations,
                                     List<String> tailCallExpressions) {
        String initialAssignments = "";
        for (int i = 0; i < paramDeclarations.size(); i++) {
            initialAssignments += String.format(template, paramDeclarations.get(i), tailCallExpressions.get(i));
        }
        return initialAssignments;
    }

    private static class ProcedureStructure {
        private final boolean negateCondition;
        private final SchemeParser.ApplicationContext tailCall;
        private final SchemeParser.ExpressionContext returnExpression;

        private ProcedureStructure(boolean negateCondition, SchemeParser.ApplicationContext tailCall,
                                   SchemeParser.ExpressionContext returnExpression) {
            this.negateCondition = negateCondition;
            this.tailCall = tailCall;
            this.returnExpression = returnExpression;
        }

    }

}

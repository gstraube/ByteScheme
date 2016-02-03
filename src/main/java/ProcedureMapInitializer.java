import java.util.*;
import java.util.stream.Collectors;

public class ProcedureMapInitializer {

    private static final String DISPLAY_PROCEDURE_NAME = "display";
    private static final String LIST_PROCEDURE_NAME = "list";
    private static final String CAR_PROCEDURE_NAME = "car";
    private static final String CDR_PROCEDURE_NAME = "cdr";
    private static final int LESS_THAN = -1;
    private static final int EQUAL = 0;
    private static final int GREATER_THAN = 1;
    private CodeGenVisitor codeGenVisitor;

    public ProcedureMapInitializer(CodeGenVisitor codeGenVisitor) {
        this.codeGenVisitor = codeGenVisitor;
    }

    public Map<String, CodeGenProcedure> getInitialMap() {
        Map<String, CodeGenProcedure> procedureMap = new HashMap<>();

        procedureMap.put(DISPLAY_PROCEDURE_NAME, expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 1) {
                SchemeParser.ExpressionContext argument = expressions.get(0);

                String mainMethodStatement = "System.out.println(OutputFormatter.output(%s));";
                GeneratedCode.GeneratedCodeBuilder genCodeBuilder = codeGenVisitor.expressionToCode().apply(argument);
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
                        .map(codeGenVisitor.expressionToCode())
                        .map(GeneratedCode.GeneratedCodeBuilder::getGeneratedCode)
                        .collect(Collectors.joining(","));
                listCode = String.format("ListWrapper.fromElements(new Object[]{%s})", listArguments);
            }
            codeBuilder.setGeneratedCode(listCode);

            return codeBuilder;
        });

        procedureMap.put(CAR_PROCEDURE_NAME, createListProcedure("car"));
        procedureMap.put(CDR_PROCEDURE_NAME, createListProcedure("cdr"));

        procedureMap.put("+", codeGenVisitor.createProcedure("PredefinedProcedures.add", "%s(new Object[]{%s})"));
        procedureMap.put("-", createChainedProcedure("PredefinedProcedures.subtract", "PredefinedProcedures.negate"));
        procedureMap.put("*", codeGenVisitor.createProcedure("PredefinedProcedures.multiply", "%s(new Object[]{%s})"));
        procedureMap.put("quotient", codeGenVisitor.createProcedure("PredefinedProcedures.divide",
                "%s(new Object[]{%s})"));
        procedureMap.put("<", createComparisonProcedure(LESS_THAN));
        procedureMap.put("<=", createComparisonProcedure(LESS_THAN, EQUAL));
        procedureMap.put(">", createComparisonProcedure(GREATER_THAN));
        procedureMap.put(">=", createComparisonProcedure(GREATER_THAN, EQUAL));

        procedureMap.put("equal?", expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 2) {
                String firstArgument = codeGenVisitor.expressionToCode().apply(expressions.get(0)).getGeneratedCode();
                String secondArgument = codeGenVisitor.expressionToCode().apply(expressions.get(1)).getGeneratedCode();

                codeBuilder.setGeneratedCode(String.format("java.util.Objects.equals(%s,%s)", firstArgument,
                        secondArgument));
            }

            return codeBuilder;
        });

        return procedureMap;
    }

    private CodeGenProcedure createListProcedure(String procedureName) {
        return expressions -> {
            GeneratedCode.GeneratedCodeBuilder codeBuilder = new GeneratedCode.GeneratedCodeBuilder();

            if (expressions.size() == 1) {
                if (expressions.get(0).application() != null) {
                    String list = codeGenVisitor.visitApplication(expressions.get(0).application()).getGeneratedCode();

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
                        codeGenVisitor.expressionToCode().apply(expressions.get(current)).getGeneratedCode(),
                        codeGenVisitor.expressionToCode().apply(expressions.get(next)).getGeneratedCode());

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
                return codeGenVisitor.createProcedure(singleArgumentProcedure,
                        "%s(new Object[]{%s})").generateCode(expressions);
            } else {
                return codeGenVisitor.createProcedure(procedureName, "%s(new Object[]{%s})").generateCode(expressions);
            }
        };
    }

}

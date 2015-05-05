import lang.*;
import lang.Vector;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

public class ParserTestVisitor extends SchemeBaseVisitor<List<String>> {

    private static final String NO_VALUE = "";

    public Map<String, Datum> variableDefinitions = new HashMap<>();
    private Map<String, Procedure> definedProcedures = new HashMap<>();

    public ParserTestVisitor() {
        definedProcedures.putAll(PredefinedProcedures.MATH_PROCEDURES);
    }

    @Override
    public List<String> visitExpression(SchemeParser.ExpressionContext expression) {
        return Arrays.asList(evaluateExpression(expression).getText());
    }

    @Override
    public List<String> visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        processVariableDefinitions(variableDefinition);
        return Collections.emptyList();
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

    private String processVariableDefinitions(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();
        Datum datum = evaluateExpression(variableDefinition.expression());
        variableDefinitions.put(identifier, datum);
        return NO_VALUE;
    }

    private Datum evaluateExpression(SchemeParser.ExpressionContext expression) {
        if (expression.constant() != null) {
            return extractConstant(expression.constant());
        }
        if (expression.quotation() != null) {
            return applyQuotation(expression.quotation());
        }
        if (expression.application() != null) {
            return evaluateApplication(expression.application());
        }
        return evaluateVariable(expression.IDENTIFIER());
    }

    private Datum evaluateApplication(SchemeParser.ApplicationContext application) {
        String procedureName = application.IDENTIFIER().getText();

        if (!definedProcedures.containsKey(procedureName)) {
            throw new ParseCancellationException(String.format("Undefined procedure %s", procedureName));
        } else {
            Procedure procedure = definedProcedures.get(procedureName);
            List<Datum> arguments = application.expression()
                    .stream()
                    .map(this::evaluateExpression)
                    .collect(Collectors.toList());
            return procedure.apply(arguments);
        }
    }

    private Datum applyQuotation(SchemeParser.QuotationContext quotation) {
        Datum datum;
        if (quotation.datum() != null) {
            SchemeParser.DatumContext datumContext = quotation.datum();

            if (datumContext.constant() != null) {
                datum = extractConstant(datumContext.constant());
            } else if (datumContext.list() != null) {
                datum = new SList(collectElements(datumContext.list().datum()));
            } else if (datumContext.vector() != null) {
                datum = new Vector(collectElements(datumContext.vector().datum()));
            } else {
                /*
                    TODO
                    Quoted strings are symbols in Scheme
                 */
                String value = datumContext.IDENTIFIER().getText();
                datum = new Constant<>(value, value);
            }

        } else {
            datum = applyQuotation(quotation.quotation());
        }

        return new Quotation(datum);
    }

    private Datum evaluateVariable(TerminalNode identifier) {
        String variableIdentifier = identifier.getText();
        if (variableDefinitions.containsKey(variableIdentifier)) {
            return variableDefinitions.get(variableIdentifier);
        }
        throw new ParseCancellationException(String.format("Undefined variable %s", variableIdentifier));
    }

    private List<Datum> collectElements(List<SchemeParser.DatumContext> data) {
        List<Datum> elements = new ArrayList<>();
        for (SchemeParser.DatumContext datum : data) {
            if (datum.list() != null) {
                elements.add(new SList(collectElements(datum.list().datum())));
            }
            if (datum.vector() != null) {
                elements.add(new Vector(collectElements(datum.vector().datum())));
            }
            if (datum.constant() != null) {
                elements.add(extractConstant(datum.constant()));
            }
            if (datum.IDENTIFIER() != null) {
                String value = datum.IDENTIFIER().getText();
                elements.add(new Constant<>(value, value));
            }
        }
        return elements;
    }

    private Constant extractConstant(SchemeParser.ConstantContext constant) {
        if (constant.NUMBER() != null) {
            String text = constant.NUMBER().getText();
            int value = Integer.valueOf(text);
            return new Constant<>(value, text);
        }
        if (constant.CHARACTER() != null) {
            String text = constant.CHARACTER().getText();
            char value = text.charAt(2);
            return new Constant<>(value, text);
        }
        if (constant.STRING() != null) {
            String text = constant.STRING().getText();
            return new Constant<>(text, text);
        }
        String text = constant.BOOLEAN().getText();
        boolean value = Boolean.valueOf(text);
        return new Constant<>(value, text);
    }

}

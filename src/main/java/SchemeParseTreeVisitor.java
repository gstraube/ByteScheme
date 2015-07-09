import lang.*;
import lang.Vector;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

public class SchemeParseTreeVisitor extends SchemeBaseVisitor<List<String>> {

    private static final String NO_VALUE = "";

    public Map<String, Datum> variableDefinitions = new HashMap<>();
    private Map<String, Procedure> definedProcedures = new HashMap<>();
    private Map<String, Datum> localBindings = new HashMap<>();
    private Map<String, SpecialForm> specialForms = new HashMap<>();

    public SchemeParseTreeVisitor() {
        definedProcedures.putAll(PredefinedProcedures.MATH_PROCEDURES);
        definedProcedures.putAll(PredefinedProcedures.LIST_PROCEDURES);
        definedProcedures.putAll(PredefinedProcedures.EQUALITY_PROCEDURES);
        definedProcedures.putAll(PredefinedProcedures.CONDITIONALS);
        definedProcedures.putAll(PredefinedProcedures.NUMBER_COMPARATORS);

        specialForms.put("if", SpecialForm.IF);
    }

    @Override
    public List<String> visitExpression(SchemeParser.ExpressionContext expression) {
        return Collections.singletonList(evaluateExpression(expression).getText());
    }

    @Override
    public List<String> visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        processVariableDefinitions(variableDefinition);
        return Collections.emptyList();
    }

    @Override
    public List<String> visitProcedure_definition(SchemeParser.Procedure_definitionContext procedureDefinition) {
        processProcedureDefinitions(procedureDefinition);
        return Collections.emptyList();
    }

    private void processProcedureDefinitions(SchemeParser.Procedure_definitionContext procedureDefinition) {
        List<SchemeParser.DefinitionContext> definitions = procedureDefinition.definition();
        definitions.forEach(this::visitDefinition);

        String procedureName = procedureDefinition.proc_name().IDENTIFIER().getText();

        List<SchemeParser.ExpressionContext> expressions = procedureDefinition.expression();
        if (expressions.size() == 0) {
            throw new ParseCancellationException(
                    String.format("Body of procedure %s does not contain an expression", procedureName));
        } else {
            /*
                While the body of a procedure can contain multiple expressions,
                only the value of the last expression is returned when calling
                the procedure. The evaluation of the other expressions can be used
                for side effects such as printing.
             */
            SchemeParser.ExpressionContext lastExpression = expressions.get(expressions.size() - 1);

            List<SchemeParser.ParamContext> parameters = procedureDefinition.param();
            if (lastExpression.application() != null) {
                defineProcedure(procedureName, lastExpression, parameters);
            } else {
                final Datum value;
                if (lastExpression.constant() != null) {
                    value = extractConstant(lastExpression.constant());
                } else if (lastExpression.IDENTIFIER() != null) {
                    value = evaluateVariable(lastExpression.IDENTIFIER());
                } else if (lastExpression.quotation() != null) {
                    value = applyQuotation(lastExpression.quotation());
                } else {
                    throw new ParseCancellationException(
                            String.format("Could not evaluate body of procedure '%s'", procedureName));
                }
                definedProcedures.put(procedureName, arguments -> value);
            }
        }
    }

    private void defineProcedure(String procedureName, SchemeParser.ExpressionContext lastExpression, List<SchemeParser.ParamContext> parameters) {
        Procedure procedure = arguments -> {
            if (parameters.size() != arguments.size()) {
                throw new ParseCancellationException(
                        String.format("Expected %d argument(s) but got %d argument(s)",
                                parameters.size(), arguments.size()));
            }

            int i = 0;
            Map<String, Datum> currentValues = new HashMap<>();
            for (SchemeParser.ParamContext parameter : parameters) {
                String parameterName = parameter.getText();
                if (localBindings.containsKey(parameterName)) {
                    currentValues.put(parameterName, localBindings.get(parameterName));
                }
                localBindings.put(parameterName, arguments.get(i));
                i++;
            }

            Datum value = evaluateApplication(lastExpression.application());

            localBindings.putAll(currentValues);

            return value;
        };
        definedProcedures.put(procedureName, procedure);
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

        if (!definedProcedures.containsKey(procedureName) && !specialForms.containsKey(procedureName)) {
            throw new ParseCancellationException(String.format("Undefined procedure '%s'", procedureName));
        } else {
            if (specialForms.containsKey(procedureName)) {
                SpecialForm specialForm = specialForms.get(procedureName);
                switch (specialForm) {
                    case IF:
                        return evaluateIfConditional(application.expression());
                    default:
                        throw new ParseCancellationException("Could not find matching special form");
                }

            } else {
                Procedure procedure = definedProcedures.get(procedureName);
                List<Datum> arguments = application.expression()
                        .stream()
                        .map(this::evaluateExpression)
                        .collect(Collectors.toList());
                return procedure.apply(arguments);
            }
        }
    }

    private Datum evaluateIfConditional(List<SchemeParser.ExpressionContext> expressions) {
        Util.checkExactArity(expressions.size(), 3);

        boolean hasBooleanCondition = false;
        boolean condition = true;

        Datum firstArgument = evaluateExpression(expressions.get(0));
        if (firstArgument instanceof Constant) {
            Constant constant = (Constant) firstArgument;
            if (constant.getValue() instanceof Boolean) {
                hasBooleanCondition = true;
                condition = (Boolean) constant.getValue();
            }
        }

            /*
                The value of the third argument (the else branch) is only
                returned when the first argument evaluates to #f.

                An except from R5RS standard (http://www.schemers.org/Documents/Standards/R5RS/,
                section "6.3.1 Booleans"):

                "Of all the standard Scheme values, only #f counts as false in conditional expressions.
                 Except for #f, all standard Scheme values, including #t, pairs, the empty list, symbols,
                 numbers, strings, vectors, and procedures, count as true."
             */
        SchemeParser.ExpressionContext returnExpression;
        if (!hasBooleanCondition || condition) {
            returnExpression = expressions.get(1);
        } else {
            returnExpression = expressions.get(2);
        }
        return evaluateExpression(returnExpression);
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
            datum = new Quotation(applyQuotation(quotation.quotation()));
        }

        return datum;
    }

    private Datum evaluateVariable(TerminalNode identifier) {
        String variableIdentifier = identifier.getText();
        if (localBindings.containsKey(variableIdentifier)) {
            return localBindings.get(variableIdentifier);
        }
        if (variableDefinitions.containsKey(variableIdentifier)) {
            return variableDefinitions.get(variableIdentifier);
        }
        throw new ParseCancellationException(String.format("Undefined variable '%s'", variableIdentifier));
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
        boolean value = text.equals("#t");
        return new Constant<>(value, text);
    }

}

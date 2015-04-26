import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParserTestVisitor extends SchemeBaseVisitor<String> {

    private static final String NO_VALUE = "";
    private static final String LIST_START = "(";
    private static final String LIST_END = ")";
    private static final String VECTOR_START = "#(";
    private static final String VECTOR_END = ")";
    private static final String QUOTATION_SYMBOL = "'";

    public Map<String, String> variableDefinitions = new HashMap<>();

    @Override
    public String visitExpression(SchemeParser.ExpressionContext expression) {
        return evaluateExpression(expression);
    }

    @Override
    public String visitVariable_definition(SchemeParser.Variable_definitionContext variableDefinition) {
        return processVariableDefinitions(variableDefinition);
    }

    @Override
    protected String defaultResult() {
        return NO_VALUE;
    }

    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        return aggregate + nextResult;
    }

    private String processVariableDefinitions(SchemeParser.Variable_definitionContext variableDefinition) {
        String identifier = variableDefinition.IDENTIFIER().getText();
        String value = evaluateExpression(variableDefinition.expression());
        variableDefinitions.put(identifier, value);
        return NO_VALUE;
    }

    private String evaluateExpression(SchemeParser.ExpressionContext expression) {
        if (expression.constant() != null) {
            return extractConstant(expression.constant());
        }
        if (expression.quotation() != null) {
            return applyQuotation(expression.quotation());
        }
        String identifier = expression.IDENTIFIER().getText();
        if (variableDefinitions.containsKey(identifier)) {
            return variableDefinitions.get(identifier);
        }
        throw new ParseCancellationException("Undefined variable");
    }

    private String applyQuotation(SchemeParser.QuotationContext quotation) {

        if (quotation.datum() != null) {
            SchemeParser.DatumContext datum = quotation.datum();

            if (datum.constant() != null) {
                return extractConstant(datum.constant());
            }

            if (datum.list() != null) {
                return collectListElements(datum.list());
            }

            if (datum.vector() != null) {
                return collectVectorElements(datum.vector());
            }

            return datum.IDENTIFIER().getText();
        }

        return QUOTATION_SYMBOL + applyQuotation(quotation.quotation());
    }

    private String collectVectorElements(SchemeParser.VectorContext vector) {
        return collectSequenceElements(vector.datum(), VECTOR_START, VECTOR_END);
    }

    private String collectListElements(SchemeParser.ListContext list) {
        return collectSequenceElements(list.datum(), LIST_START, LIST_END);
    }

    private String collectSequenceElements(List<SchemeParser.DatumContext> data,
                                           String seqStartSymbol,
                                           String seqEndSymbol) {
        String sequence = seqStartSymbol;
        sequence += collectData(data);
        sequence += seqEndSymbol;
        return sequence;
    }

    private String collectData(List<SchemeParser.DatumContext> data) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            SchemeParser.DatumContext datum = data.get(i);
            if (datum.list() != null) {
                builder.append(collectListElements(datum.list()));
            }
            if (datum.vector() != null) {
                builder.append(collectVectorElements(datum.vector()));
            }
            if (datum.constant() != null) {
                builder.append(extractConstant(datum.constant()));
            }
            if (datum.IDENTIFIER() != null) {
                builder.append(datum.IDENTIFIER().getText());
            }
            if (i < data.size() - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    private String extractConstant(SchemeParser.ConstantContext constant) {
        if (constant.NUMBER() != null) {
            return constant.NUMBER().getText();
        }
        if (constant.CHARACTER() != null) {
            return constant.CHARACTER().getText();
        }
        if (constant.STRING() != null) {
            return constant.STRING().getText();
        }
        return constant.BOOLEAN().getText();
    }

}

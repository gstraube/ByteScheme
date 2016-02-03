import java.util.List;

public interface CodeGenProcedure {

    GeneratedCode.GeneratedCodeBuilder generateCode(List<SchemeParser.ExpressionContext> expression);

}

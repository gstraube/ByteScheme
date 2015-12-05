import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import parser.ErrorListener;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CodeGenVisitorTest {

    CodeGenVisitor codeGenVisitor;

    @Test
    public void constant_expressions_are_processed_correctly() {
        String input = "20414342334 \"a string\" #\\λ #t #f #\\newline #\\space";
        GeneratedCode generatedCode = visitParseTreeForInput(input);

        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("new java.math.BigInteger(\"20414342334\");");
        expectedOutput.add("new String(\"a string\");");
        expectedOutput.add("new Character('λ');");
        expectedOutput.add("new Boolean(true);");
        expectedOutput.add("new Boolean(false);");
        expectedOutput.add("new Character('\\n');");
        expectedOutput.add("new Character(' ');");

        assertThat(generatedCode.getConstants(), is(expectedOutput));
    }

    @Test
    public void variable_definitions_with_constants_are_processed_correctly() {
        String input = "(define var0 51) (define var1 \"a string\")" +
                "(define var2 #\\λ) (define var3 #\\newline) (define var4 #t) (define var5 #f)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("java.math.BigInteger var0 = new java.math.BigInteger(\"51\");");
        expectedOutput.add("String var1 = new String(\"a string\");");
        expectedOutput.add("Character var2 = new Character('λ');");
        expectedOutput.add("Character var3 = new Character('\\n');");
        expectedOutput.add("Boolean var4 = new Boolean(true);");
        expectedOutput.add("Boolean var5 = new Boolean(false);");

        assertThat(generatedCode.getVariableDefinitions(), is(expectedOutput));
    }

    @Test
    public void correct_code_is_generated_for_variable_definitions_referencing_other_variables() {
        String input = "(define var1 \"a string\") (define var2 var1)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        assertThat(generatedCode.getVariableDefinitions().get(0),
                is("String var1 = new String(\"a string\");"));
        assertThat(generatedCode.getVariableDefinitions().get(1),
                is("String var2 = var1;"));
    }

    private GeneratedCode visitParseTreeForInput(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        SchemeLexer lexer = new SchemeLexer(inputStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SchemeParser parser = new SchemeParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ErrorListener.INSTANCE);

        ParseTree parseTree = parser.program();
        codeGenVisitor = new CodeGenVisitor();
        return codeGenVisitor.visit(parseTree).build();
    }

}
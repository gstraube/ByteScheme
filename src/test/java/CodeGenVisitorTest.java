import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import parser.ErrorListener;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CodeGenVisitorTest {

    CodeGenVisitor codeGenVisitor;

    @Test
    public void top_level_constant_expressions_are_processed_correctly() {
        String input = "20414342334 \"a string\" #\\λ #t #f #\\newline #\\space";
        GeneratedCode generatedCode = visitParseTreeForInput(input);

        assertThat(generatedCode.getMethodsToBeCalled().size(), is(7));
        assertThat(generatedCode.getMethodsToBeDeclared().size(), is(7));

        String expectedOutput = "printConstant%d";
        for (int i = 0; i < 7; i++) {
            assertThat(generatedCode.getMethodsToBeCalled().get(i), is(String.format(expectedOutput, i)));
        }

        expectedOutput = "public String printConstant%d(){return String.valueOf(%s);}";
        assertThat(generatedCode.getMethodsToBeDeclared().get(0), is(String.format(expectedOutput,
                0, "new java.math.BigInteger(\"20414342334\")")));
        assertThat(generatedCode.getMethodsToBeDeclared().get(1), is(String.format(expectedOutput,
                1, "\"a string\"")));
        assertThat(generatedCode.getMethodsToBeDeclared().get(2), is(String.format(expectedOutput,
                2, "'λ'")));
        assertThat(generatedCode.getMethodsToBeDeclared().get(3), is(String.format(expectedOutput,
                3, "true")));
        assertThat(generatedCode.getMethodsToBeDeclared().get(4), is(String.format(expectedOutput,
                4, "false")));
        assertThat(generatedCode.getMethodsToBeDeclared().get(5), is(String.format(expectedOutput,
                5, "'\n'")));
        assertThat(generatedCode.getMethodsToBeDeclared().get(6), is(String.format(expectedOutput,
                6, "' '")));
    }

    @Test
    public void variable_definitions_with_constants_are_processed_correctly() {
        String input = "(define var1 51) (define var2 \"a string\")" +
                "(define var3 #\\λ) (define var4 #\\newline) (define var5 #t) (define var6 #f)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        List<String> variableDefinitions = generatedCode.getVariableDefinitions();
        assertThat(variableDefinitions.size(), is(6));

        assertThat(variableDefinitions.get(0),
                is("java.math.BigInteger var1 = new java.math.BigInteger(\"51\");"));
        assertThat(variableDefinitions.get(1), is("String var2 = \"a string\";"));
        assertThat(variableDefinitions.get(2), is("char var3 = 'λ';"));
        assertThat(variableDefinitions.get(3), is("char var4 = '\n';"));
        assertThat(variableDefinitions.get(4), is("boolean var5 = true;"));
        assertThat(variableDefinitions.get(5), is("boolean var6 = false;"));
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
        return codeGenVisitor.visit(parseTree);
    }

}
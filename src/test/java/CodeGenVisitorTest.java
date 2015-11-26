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

        expectedOutput = "public String printConstant%d(){return OutputFormatter.output(%s);}";
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
        String input = "(define var0 51) (define var1 \"a string\")" +
                "(define var2 #\\λ) (define var3 #\\newline) (define var4 #t) (define var5 #f)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        List<String> variableDefinitions = generatedCode.getVariableDefinitions();
        assertThat(variableDefinitions.size(), is(6));

        assertThat(variableDefinitions.get(0),
                is("java.math.BigInteger var0 = new java.math.BigInteger(\"51\");"));
        assertThat(variableDefinitions.get(1), is("String var1 = \"a string\";"));
        assertThat(variableDefinitions.get(2), is("char var2 = 'λ';"));
        assertThat(variableDefinitions.get(3), is("char var3 = '\n';"));
        assertThat(variableDefinitions.get(4), is("boolean var4 = true;"));
        assertThat(variableDefinitions.get(5), is("boolean var5 = false;"));

        input = "var0 var1 var2 var3 var4 var5";
        generatedCode = visitParseTreeForInput(input);

        String expectedOutput = "public String %s(){return OutputFormatter.output(%s);}";
        assertThat(generatedCode.getMethodsToBeCalled().size(), is(6));
        for (int i = 0; i < 6; i++) {
            String variableIdentifier = String.format("var%d", i);
            assertThat(generatedCode.getMethodsToBeCalled().get(i), is(variableIdentifier));
            assertThat(generatedCode.getMethodsToBeDeclared().get(i),
                    is(String.format(expectedOutput, variableIdentifier, variableIdentifier)));
        }
    }

    @Test
    public void correct_code_is_generated_for_variable_definitions_referencing_other_variables() {
        String input = "(define var1 \"a string\") (define var2 var1)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        List<String> variableDefinitions = generatedCode.getVariableDefinitions();
        assertThat(variableDefinitions.size(), is(2));

        assertThat(variableDefinitions.get(0), is("String var1 = \"a string\";"));
        assertThat(variableDefinitions.get(1), is("String var2 = var1;"));
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
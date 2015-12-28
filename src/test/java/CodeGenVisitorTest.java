import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.hamcrest.Matchers;
import org.junit.Test;
import parser.ErrorListener;

import java.util.ArrayList;
import java.util.Collections;
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
        expectedOutput.add("static java.math.BigInteger var0 = new java.math.BigInteger(\"51\");");
        expectedOutput.add("static String var1 = new String(\"a string\");");
        expectedOutput.add("static Character var2 = new Character('λ');");
        expectedOutput.add("static Character var3 = new Character('\\n');");
        expectedOutput.add("static Boolean var4 = new Boolean(true);");
        expectedOutput.add("static Boolean var5 = new Boolean(false);");

        assertThat(generatedCode.getVariableDefinitions(), is(expectedOutput));
    }

    @Test
    public void correct_code_is_generated_for_variable_definitions_referencing_other_variables() {
        String input = "(define var1 \"a string\") (define var2 var1)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        assertThat(generatedCode.getVariableDefinitions().get(0),
                is("static String var1 = new String(\"a string\");"));
        assertThat(generatedCode.getVariableDefinitions().get(1),
                is("static String var2 = var1;"));
    }

    @Test
    public void an_application_of_the_display_procedure_results_in_an_output_statement_in_main_method() {
        String input = "(display \"a string\")";
        input += "(define var 14334234) (display var)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        assertThat(generatedCode.getVariableDefinitions().get(0),
                is("static java.math.BigInteger var = new java.math.BigInteger(\"14334234\");"));

        assertThat(generatedCode.getMethodsToBeDeclared().size(), is(1));
        assertThat(generatedCode.getMethodsToBeDeclared().get(0), is("public static void main(String[] args){" +
                "System.out.println(OutputFormatter.output(new String(\"a string\")));" +
                "System.out.println(OutputFormatter.output(var));}"));
    }

    @Test
    public void invoking_the_list_procedure_on_constants_yields_a_list_wrapper() {
        String input = "(list 15 7 #\\u #f \"a string\")";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        String expectedOutput = "ListWrapper.fromElements(new Object[]{new java.math.BigInteger(\"15\")," +
                "new java.math.BigInteger(\"7\"),new Character('u'),new Boolean(false),new String(\"a string\")})";

        assertThat(generatedCode.getConstants(), is(Collections.singletonList(expectedOutput)));
    }

    @Test
    public void previously_defined_variables_can_be_referenced_as_arguments_to_the_list_procedure() {
        String input = "(define a_variable \"a string\") (list #\\u #f a_variable)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        String expectedOutput = "ListWrapper.fromElements(new Object[]{new Character('u'),new Boolean(false)," +
                "a_variable})";

        assertThat(generatedCode.getConstants(), is(Collections.singletonList(expectedOutput)));
    }

    @Test
    public void lists_can_be_nested() {
        String input = "(list 15 (list \"abc\" #t) 7 (list #\\u #f) 2 \"a string\")";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        String expectedOutput = "ListWrapper.fromElements(new Object[]{new java.math.BigInteger(\"15\")," +
                "ListWrapper.fromElements(new Object[]{new String(\"abc\"),new Boolean(true)}),new java.math.BigInteger(\"7\")," +
                "ListWrapper.fromElements(new Object[]{new Character('u'),new Boolean(false)})," +
                "new java.math.BigInteger(\"2\"),new String(\"a string\")})";

        assertThat(generatedCode.getConstants(), is(Collections.singletonList(expectedOutput)));
    }

    @Test
    public void predefined_procedures_can_be_called() {
        String input = "(+ 2 3 (+ 3 7) 6)";
        assertThat(visitParseTreeForInput(input).getConstants().get(0),
                Matchers.is("new java.math.BigInteger(\"2\").add(new java.math.BigInteger(\"3\"))" +
                        ".add(new java.math.BigInteger(\"3\").add(new java.math.BigInteger(\"7\")))" +
                        ".add(new java.math.BigInteger(\"6\"))"));


        input = "(- 10 (- 5 200) 375 (- 20))";
        assertThat(visitParseTreeForInput(input).getConstants().get(0), Matchers.is("new java.math.BigInteger(\"10\").subtract(new java.math.BigInteger(\"5\")" +
                ".subtract(new java.math.BigInteger(\"200\"))).subtract(new java.math.BigInteger(\"375\"))" +
                ".subtract(new java.math.BigInteger(\"20\").negate())"));

        input = "(* 2 10 (* 3 7))";
        assertThat(visitParseTreeForInput(input).getConstants().get(0), Matchers.is("new java.math.BigInteger(\"2\")" +
                ".multiply(new java.math.BigInteger(\"10\")).multiply(new java.math.BigInteger(\"3\")" +
                ".multiply(new java.math.BigInteger(\"7\")))"));


        input = "(quotient 10 (quotient 7 3))";
        assertThat(visitParseTreeForInput(input).getConstants().get(0), Matchers.is("new java.math.BigInteger(\"10\")" +
                ".divide(new java.math.BigInteger(\"7\").divide(new java.math.BigInteger(\"3\")))"));
    }

    @Test
    public void an_empty_list_can_be_created() {
        assertThat(visitParseTreeForInput("(list)").getConstants().get(0),
                is("ListWrapper.fromElements(new Object[0])"));
    }

    @Test
    public void the_equal_procedure_is_implemented_by_the_equals_method_defined_in_the_Objects_class() {
        String input = "(equal? 42 \"forty-two\")";
        assertThat(visitParseTreeForInput(input).getConstants().get(0),
                Matchers.is("new Boolean(java.util.Objects.equals(new java.math.BigInteger(\"42\")," +
                        "new String(\"forty-two\")))"));
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
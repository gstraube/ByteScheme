import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.hamcrest.Matchers;
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
        String[] inputs = {"20414342334", "\"a string\"", "#\\λ", "#t", "#f", "#\\newline", "#\\space"};
        String[] expectedOutputs = {"new BigInteger(\"20414342334\");", "new String(\"a string\");",
                "new Character('λ');", "new Boolean(true);", "new Boolean(false);", "new Character('\\n');",
                "new Character(' ');"};
        for (int i = 0; i < inputs.length; i++) {
            GeneratedCode generatedCode = visitParseTreeForInput(inputs[i]);
            assertThat(generatedCode.getGeneratedCode(), is(expectedOutputs[i]));
        }
    }

    @Test
    public void variable_definitions_with_constants_are_processed_correctly() {
        String input = "(define var0 51) (define var1 \"a string\")" +
                "(define var2 #\\λ) (define var3 #\\newline) (define var4 #t) (define var5 #f)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);

        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("static BigInteger var0 = new BigInteger(\"51\");");
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
                is("static BigInteger var = new BigInteger(\"14334234\");"));

        assertThat(generatedCode.getMethodsToBeDeclared().size(), is(1));
        assertThat(generatedCode.getMethodsToBeDeclared().get(0), is("public static void main(String[] args){" +
                "System.out.println(OutputFormatter.output(new String(\"a string\")));" +
                "System.out.println(OutputFormatter.output(var));}"));
    }

    @Test
    public void invoking_the_list_procedure_on_constants_yields_a_list_wrapper() {
        String input = "(list 15 7 #\\u #f \"a string\")";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        String expectedOutput = "ListWrapper.fromElements(new Object[]{new BigInteger(\"15\")," +
                "new BigInteger(\"7\"),new Character('u'),new Boolean(false),new String(\"a string\")})";

        assertThat(generatedCode.getGeneratedCode(), is(expectedOutput));
    }

    @Test
    public void previously_defined_variables_can_be_referenced_as_arguments_to_the_list_procedure() {
        String input = "(define a_variable \"a string\") (list #\\u #f a_variable)";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        String expectedOutput = "ListWrapper.fromElements(new Object[]{new Character('u'),new Boolean(false)," +
                "a_variable})";

        assertThat(generatedCode.getGeneratedCode(), is(expectedOutput));
    }

    @Test
    public void lists_can_be_nested() {
        String input = "(list 15 (list \"abc\" #t) 7 (list #\\u #f) 2 \"a string\")";

        GeneratedCode generatedCode = visitParseTreeForInput(input);
        String expectedOutput = "ListWrapper.fromElements(new Object[]{new BigInteger(\"15\")," +
                "ListWrapper.fromElements(new Object[]{new String(\"abc\"),new Boolean(true)}),new BigInteger(\"7\")," +
                "ListWrapper.fromElements(new Object[]{new Character('u'),new Boolean(false)})," +
                "new BigInteger(\"2\"),new String(\"a string\")})";

        assertThat(generatedCode.getGeneratedCode(), is(expectedOutput));
    }

    @Test
    public void predefined_procedures_can_be_called() {
        String input = "(+ 2 3 (+ 3 7) 6)";
        assertThat(visitParseTreeForInput(input).getGeneratedCode(),
                Matchers.is("PredefinedProcedures.add(new Object[]{new BigInteger(\"2\")," +
                        "new BigInteger(\"3\")," +
                        "PredefinedProcedures.add(new Object[]{new BigInteger(\"3\")," +
                        "new BigInteger(\"7\")}),new BigInteger(\"6\")})"));

        input = "(- 10 (- 5 200) 375 (- 20))";
        assertThat(visitParseTreeForInput(input).getGeneratedCode(),
                Matchers.is("PredefinedProcedures.subtract(new Object[]{new BigInteger(\"10\")," +
                        "PredefinedProcedures.subtract(new Object[]{new BigInteger(\"5\")," +
                        "new BigInteger(\"200\")}),new BigInteger(\"375\")," +
                        "PredefinedProcedures.negate(new Object[]{new BigInteger(\"20\")})})"));

        input = "(* 2 10 (* 3 7))";
        assertThat(visitParseTreeForInput(input).getGeneratedCode(),
                Matchers.is("PredefinedProcedures.multiply(new Object[]{new BigInteger(\"2\")," +
                        "new BigInteger(\"10\")," +
                        "PredefinedProcedures.multiply(new Object[]{new BigInteger(\"3\")," +
                        "new BigInteger(\"7\")})})"));

        input = "(quotient 10 (quotient 7 3))";
        assertThat(visitParseTreeForInput(input).getGeneratedCode(),
                Matchers.is("PredefinedProcedures.divide(new Object[]{new BigInteger(\"10\")," +
                        "PredefinedProcedures.divide(new Object[]{new BigInteger(\"7\")," +
                        "new BigInteger(\"3\")})})"));
    }

    @Test
    public void an_empty_list_can_be_created() {
        assertThat(visitParseTreeForInput("(list)").getGeneratedCode(),
                is("ListWrapper.fromElements(new Object[0])"));
    }

    @Test
    public void the_equal_procedure_is_implemented_by_the_equals_method_defined_in_the_Objects_class() {
        String input = "(equal? 42 \"forty-two\")";
        assertThat(visitParseTreeForInput(input).getGeneratedCode(),
                Matchers.is("java.util.Objects.equals(new BigInteger(\"42\")," +
                        "new String(\"forty-two\"))"));
    }

    @Test
    public void it_is_possible_to_define_a_procedure_which_returns_a_constant() {
        String input = "(define (the_answer) 42) (display (the_answer))";
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(0),
                Matchers.is("public static Object the_answer(){return new BigInteger(\"42\");}"));
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(1),
                Matchers.is("public static void main(String[] args)" +
                        "{System.out.println(OutputFormatter.output(the_answer()));}"));
    }

    @Test
    public void it_is_possible_to_define_a_procedure_which_references_a_variable() {
        String input = "(define foo 123) (define (bar) foo) (display (bar))";
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(0),
                Matchers.is("public static Object bar(){return foo;}"));
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(1),
                Matchers.is("public static void main(String[] args)" +
                        "{System.out.println(OutputFormatter.output(bar()));}"));
    }

    @Test
    public void it_is_possible_to_define_a_procedure_which_contains_a_procedure_application() {
        String input = "(define (double_arg x) (* x 2)) (display (double_arg 20))";
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(0),
                Matchers.is("public static Object double_arg(Object x)" +
                        "{return PredefinedProcedures.multiply(new Object[]{x,new BigInteger(\"2\")});}"));
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(1),
                Matchers.is("public static void main(String[] args)" +
                        "{System.out.println(OutputFormatter.output(double_arg(new BigInteger(\"20\"))));}"));
    }

    @Test
    public void it_is_possible_to_define_a_procedure_containing_the_if_special_form() {
        String input = "(define (is_42 x) (if (equal? x 42) \"yes\" \"no\"))";
        assertThat(visitParseTreeForInput(input).getMethodsToBeDeclared().get(0),
                Matchers.is("public static Object is_42(Object x)" +
                        "{if(java.util.Objects.equals(x,new BigInteger(\"42\"))){return new String(\"yes\");}" +
                        "else{return new String(\"no\");}}"));
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
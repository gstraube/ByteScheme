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
    public void constants_are_processed_correctly() {
        assertThat(visitParseTreeForInput("20414342334"), is("new java.math.BigInteger(\"20414342334\")"));
        assertThat(visitParseTreeForInput("\"a string\""), is("\"a string\""));
        assertThat(visitParseTreeForInput("#\\λ"), is("'λ'"));
        assertThat(visitParseTreeForInput("#t"), is("true"));
        assertThat(visitParseTreeForInput("#f"), is("false"));
    }

    @Test
    public void quoting_a_flat_list_with_constants_yields_a_class_in_the_generated_code() {
        String input = "'(1 #t \"a string\" #\\d \"another string\" 4343323232324)";

        String expectedOutput = "class SList{";
        expectedOutput += "java.math.BigInteger e0 = new java.math.BigInteger(\"1\");";
        expectedOutput += "boolean e1 = true;";
        expectedOutput += "String e2 = \"a string\";";
        expectedOutput += "char e3 = 'd';";
        expectedOutput += "String e4 = \"another string\";";
        expectedOutput += "java.math.BigInteger e5 = new java.math.BigInteger(\"4343323232324\");}";

        assertThat(visitParseTreeForInput(input), is(expectedOutput));
    }

    @Test
    public void variable_definitions_with_constants_are_processed_correctly() {
        assertThat(visitParseTreeForInput("(define var1 51)"),
                is("java.math.BigInteger var1 = new java.math.BigInteger(\"51\");"));
        assertThat(visitParseTreeForInput("(define var2 \"a string\")"), is("String var2 = \"a string\";"));
        assertThat(visitParseTreeForInput("(define var3 #\\λ)"), is("char var3 = 'λ';"));
        assertThat(visitParseTreeForInput("(define var4 #t)"), is("boolean var4 = true;"));
        assertThat(visitParseTreeForInput("(define var5 #f)"), is("boolean var5 = false;"));
    }

    private String visitParseTreeForInput(String input) {
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
        List<String> output = codeGenVisitor.visit(parseTree);

        return String.join(" ", output);
    }

}
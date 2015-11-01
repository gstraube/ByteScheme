import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Test;
import parser.ErrorListener;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;

public class CodeGenVisitorTest {

    CodeGenVisitor codeGenVisitor;

    @Test
    public void constants_are_processed_correctly() {
        Assert.assertThat(visitParseTreeForInput("20414342334"), is("new BigInteger(\"20414342334\")"));
        Assert.assertThat(visitParseTreeForInput("\"a string\""), is("\"a string\""));
        Assert.assertThat(visitParseTreeForInput("#\\λ"), is("'λ'"));
        Assert.assertThat(visitParseTreeForInput("#t"), is("true"));
        Assert.assertThat(visitParseTreeForInput("#f"), is("false"));
    }

    @Test
    public void quoting_a_flat_list_with_constants_yields_a_class_in_the_generated_code() {
        String input = "'(1 #t \"a string\" #\\d \"another string\" 4343323232324)";

        String expectedOutput = "class SList{";
        expectedOutput += "BigInteger e0 = new BigInteger(\"1\");";
        expectedOutput += "boolean e1 = true;";
        expectedOutput += "String e2 = \"a string\";";
        expectedOutput += "char e3 = 'd';";
        expectedOutput += "String e4 = \"another string\";";
        expectedOutput += "BigInteger e5 = new BigInteger(\"4343323232324\");}";

        Assert.assertThat(visitParseTreeForInput(input), is(expectedOutput));
    }

    @Test
    public void variable_definitions_with_constants_are_processed_correctly() {
        Assert.assertThat(visitParseTreeForInput("(define var1 51)"), is("BigInteger var1 = new BigInteger(\"51\");"));
        Assert.assertThat(visitParseTreeForInput("(define var2 \"a string\")"), is("String var2 = \"a string\";"));
        Assert.assertThat(visitParseTreeForInput("(define var3 #\\λ)"), is("char var3 = 'λ';"));
        Assert.assertThat(visitParseTreeForInput("(define var4 #t)"), is("boolean var4 = true;"));
        Assert.assertThat(visitParseTreeForInput("(define var5 #f)"), is("boolean var5 = false;"));
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
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;

public class ParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    ParserTestVisitor parserTestVisitor;

    @Before
    public void initializeVisitor() {
        parserTestVisitor = new ParserTestVisitor();
    }

    @Test
    public void constant_is_parsed_correctly() {
        visitParseTreeForInput("42");

        Assert.assertThat(parserTestVisitor.constants.size(), is(1));
        Assert.assertThat(parserTestVisitor.constants.get(0), is("42"));
    }

    @Test
    public void it_is_possible_to_define_a_variable() {
        visitParseTreeForInput("(define a_variable 51)");

        Assert.assertThat(parserTestVisitor.variableDefinitions.size(), is(1));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_variable"), is("51"));
    }

    @Test
    public void it_is_possible_to_reference_a_variable() {
        visitParseTreeForInput("(define a_variable 12) (define a_second_variable a_variable)");

        Assert.assertThat(parserTestVisitor.variableDefinitions.size(), is(2));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_variable"), is("12"));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_second_variable"), is("12"));
    }

    @Test
    public void trying_to_reference_an_undefined_variable_causes_an_exception() {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Undefined variable");

        visitParseTreeForInput("(define a_variable undefined_variable)");
    }

    private void visitParseTreeForInput(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        SchemeLexer lexer = new SchemeLexer(inputStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SchemeParser parser = new SchemeParser(tokens);

        ParseTree parseTree = parser.program();
        parserTestVisitor = new ParserTestVisitor();
        parserTestVisitor.visit(parseTree);
    }

}

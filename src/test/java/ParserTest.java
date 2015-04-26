import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import parser.ErrorListener;

import static org.hamcrest.Matchers.is;

public class ParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    ParserTestVisitor parserTestVisitor;
    public static final String[] CONSTANTS = new String[]{
            "1", "42", "-1237", "#t",
            "#\\λ", "#\\newline", "#\\space", "\"a string\""
    };

    @Before
    public void initializeVisitor() {
        parserTestVisitor = new ParserTestVisitor();
    }

    @Test
    public void constants_are_parsed_correctly() {
        for (String constant : CONSTANTS) {
            Assert.assertThat(visitParseTreeForInput(constant), is(constant));
        }
    }

    @Test
    public void quotation_marks_are_not_allowed_in_string_constants() {
        expectedException.expect(ParseCancellationException.class);

        visitParseTreeForInput("\"\"\"");
    }

    @Test
    public void backslashes_are_not_allowed_in_string_constants() {
        expectedException.expect(ParseCancellationException.class);

        visitParseTreeForInput("\"\\\"");
    }

    @Test
    public void it_is_possible_to_define_a_variable() {
        for (String constant : CONSTANTS) {
            visitParseTreeForInput(String.format("(define a_variable %s)", constant));
            Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_variable"), is(constant));
        }
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
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined variable");

        visitParseTreeForInput("(define a_variable undefined_variable)");
    }

    @Test
    public void multiple_definitions_can_be_grouped_using_the_begin_keyword() {
        visitParseTreeForInput("(begin (begin (define a \"foo\") (define b 21)) (define c #t))");

        Assert.assertThat(parserTestVisitor.variableDefinitions.size(), is(3));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a"), is("\"foo\""));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("b"), is("21"));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("c"), is("#t"));
    }

    @Test
    public void quoting_a_constant_once_returns_the_constant() {
        for (String constant : CONSTANTS) {
            String result = visitParseTreeForInput(String.format("(quote %s)", constant));
            Assert.assertThat(result, is(constant));
            result = visitParseTreeForInput(String.format("'%s", constant));
            Assert.assertThat(result, is(constant));
        }
    }

    @Test
    public void quoting_an_identifier_produces_a_symbol() {
        Assert.assertThat(visitParseTreeForInput("(quote an_identifier)"), is("an_identifier"));
    }

    @Test
    public void quoting_a_sequence_of_data_in_parentheses_produces_a_list() {
        String listElements = "(15 (\"abc\" #t) 7 #(1 2 3) (#\\u #f) 2 \"a_string\")";
        String input = "(quote " + listElements + ")";
        Assert.assertThat(visitParseTreeForInput(input), is(listElements));
    }

    @Test
    public void multiple_quotation_is_possible() {
        String input = "(quote (quote (quote (1 2 3))))";
        Assert.assertThat(visitParseTreeForInput(input), is("''(1 2 3)"));
    }

    @Test
    public void quoting_a_sequence_of_data_in_parentheses_and_prefixed_with_a_number_sign_produces_a_vector() {
        String listElements = "#(\"abc\" 1 #\\u 20 30 (#\\v #\\z #(1 2 3)))";
        String input = "(quote " + listElements + ")";
        Assert.assertThat(visitParseTreeForInput(input), is(listElements));
    }

    @Test
    public void all_character_symbols_are_supported() {
        String characterSymbolsInList = "(+ - ... !.. $.+ %.- &.! *.: /:. :+. <-. =. >. ?. ~. _. ^.)";
        Assert.assertThat(visitParseTreeForInput("'" + characterSymbolsInList), is(characterSymbolsInList));
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
        parserTestVisitor = new ParserTestVisitor();
        return parserTestVisitor.visit(parseTree);
    }

}

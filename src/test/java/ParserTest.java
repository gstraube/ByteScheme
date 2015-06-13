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

import java.util.List;

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
            Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_variable").getText(), is(constant));
        }
    }

    @Test
    public void it_is_possible_to_reference_a_variable() {
        visitParseTreeForInput("(define a_variable 12) (define a_second_variable a_variable)");

        Assert.assertThat(parserTestVisitor.variableDefinitions.size(), is(2));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_variable").getText(), is("12"));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a_second_variable").getText(), is("12"));
    }

    @Test
    public void trying_to_reference_an_undefined_variable_causes_an_exception() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined variable undefined_variable");

        visitParseTreeForInput("(define a_variable undefined_variable)");
    }

    @Test
    public void multiple_definitions_can_be_grouped_using_the_begin_keyword() {
        visitParseTreeForInput("(begin (begin (define a \"foo\") (define b 21)) (define c #t))");

        Assert.assertThat(parserTestVisitor.variableDefinitions.size(), is(3));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("a").getText(), is("\"foo\""));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("b").getText(), is("21"));
        Assert.assertThat(parserTestVisitor.variableDefinitions.get("c").getText(), is("#t"));
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

    @Test
    public void a_program_can_contain_multiple_expressions_on_the_top_level() {
        String input = "2 (define foo 3) \"bar\" foo #t (define baz #\\a) baz";
        Assert.assertThat(visitParseTreeForInput(input), is("2 \"bar\" 3 #t #\\a"));
    }

    @Test
    public void an_application_with_an_undefined_procedure_leads_to_an_error() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined procedure func");

        visitParseTreeForInput("(func 1 2 3)");
    }

    @Test
    public void predefined_procedures_can_be_called() {
        String input = "(+ 2 3 (+ 10) (+ 3 7))";
        Assert.assertThat(visitParseTreeForInput(input), is("25"));
        input = "(- 10 (- 5 200) 375 (- 20))";
        Assert.assertThat(visitParseTreeForInput(input), is("-150"));
        input = "(* 2 (* 5) 10 (* 3 7))";
        Assert.assertThat(visitParseTreeForInput(input), is("2100"));
        input = "(quotient 10 (quotient 7 3))";
        Assert.assertThat(visitParseTreeForInput(input), is("5"));
    }

    @Test
    public void passing_less_than_the_minimal_expected_number_of_arguments_to_a_procedure_causes_an_error() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Arguments count 0 does not match expected minimal arity of 1");

        visitParseTreeForInput("(+)");
    }

    @Test
    public void passing_the_wrong_number_of_arguments_to_a_procedure_causes_an_error() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Arguments count 3 does not match expected arity of 2");

        visitParseTreeForInput("(quotient 1 2 3)");
    }

    @Test
    public void car_returns_the_first_element_of_the_list() {
        String input = "(car '(1 2 3))";
        Assert.assertThat(visitParseTreeForInput(input), is("1"));
        input = "(car '((\"abc\" \"xyz\") 5 (6 7) 8))";
        Assert.assertThat(visitParseTreeForInput(input), is("(\"abc\" \"xyz\")"));
    }

    @Test
    public void car_on_the_empty_list_causes_an_exception() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Wrong argument type: Expected pair");

        visitParseTreeForInput("(car '())");
    }

    @Test
    public void cdr_returns_all_elements_of_a_list_except_the_first_one() {
        String input = "(cdr '(1 2 3))";
        Assert.assertThat(visitParseTreeForInput(input), is("(2 3)"));
        input = "(cdr '((\"abc\" \"xyz\") 5 (6 7) 8))";
        Assert.assertThat(visitParseTreeForInput(input), is("(5 (6 7) 8)"));
    }

    @Test
    public void cdr_on_the_empty_list_causes_an_exception() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Wrong argument type: Expected pair");

        visitParseTreeForInput("(cdr '())");
    }

    @Test
    public void it_is_possible_to_define_a_procedure_which_returns_a_constant() {
        String input = "(define (the_answer) 42)";
        input += "(the_answer)";
        Assert.assertThat(visitParseTreeForInput(input), is("42"));
    }

    @Test
    public void a_variable_can_be_referenced_in_the_body_of_a_procedure_definition() {
        String input = "(define foo 123) (define (bar) foo) (bar)";
        Assert.assertThat(visitParseTreeForInput(input), is("123"));
    }

    @Test
    public void a_procedure_definition_can_contain_definitions() throws Exception {
        String input = "(define (foo) (define bar \"a_string\") bar) (foo)";
        Assert.assertThat(visitParseTreeForInput(input), is("\"a_string\""));
    }

    @Test
    public void a_procedure_definition_can_contain_a_quotation_in_its_body() throws Exception {
        String input = "(define (foo) '(1 \"a_string\" (3 #t) #\\a)) (foo)";
        Assert.assertThat(visitParseTreeForInput(input), is("(1 \"a_string\" (3 #t) #\\a)"));
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
        List<String> output = parserTestVisitor.visit(parseTree);

        return  String.join(" ", output);
    }

}

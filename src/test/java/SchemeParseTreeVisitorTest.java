import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import parser.ErrorListener;

import java.util.List;

public class SchemeParseTreeVisitorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    SchemeParseTreeVisitor schemeParseTreeVisitor;
    public static final String[] CONSTANTS = new String[]{
            "1", "42", "-1237", "#t",
            "#\\λ", "#\\newline", "#\\space", "\"a string\""
    };

    @Before
    public void initializeVisitor() {
        schemeParseTreeVisitor = new SchemeParseTreeVisitor();
    }

    @Test
    public void constants_are_parsed_correctly() {
        for (String constant : CONSTANTS) {
            Assert.assertThat(visitParseTreeForInput(constant), Matchers.is(constant));
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
            Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.get("a_variable").getText(), Matchers.is(constant));
        }
    }

    @Test
    public void it_is_possible_to_reference_a_variable() {
        visitParseTreeForInput("(define a_variable 12) (define a_second_variable a_variable)");

        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.size(), Matchers.is(2));
        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.get("a_variable").getText(), Matchers.is("12"));
        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.get("a_second_variable").getText(), Matchers.is("12"));
    }

    @Test
    public void trying_to_reference_an_undefined_variable_causes_an_exception() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined variable 'undefined_variable'");

        visitParseTreeForInput("(define a_variable undefined_variable)");
    }

    @Test
    public void multiple_definitions_can_be_grouped_using_the_begin_keyword() {
        visitParseTreeForInput("(begin (begin (define a \"foo\") (define b 21)) (define c #t))");

        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.size(), Matchers.is(3));
        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.get("a").getText(), Matchers.is("\"foo\""));
        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.get("b").getText(), Matchers.is("21"));
        Assert.assertThat(schemeParseTreeVisitor.variableDefinitions.get("c").getText(), Matchers.is("#t"));
    }

    @Test
    public void quoting_a_constant_once_returns_the_constant() {
        for (String constant : CONSTANTS) {
            String result = visitParseTreeForInput(String.format("(quote %s)", constant));
            Assert.assertThat(result, Matchers.is(constant));
            result = visitParseTreeForInput(String.format("'%s", constant));
            Assert.assertThat(result, Matchers.is(constant));
        }
    }

    @Test
    public void quoting_an_identifier_produces_a_symbol() {
        Assert.assertThat(visitParseTreeForInput("(quote an_identifier)"), Matchers.is("an_identifier"));
    }

    @Test
    public void quoting_a_sequence_of_data_in_parentheses_produces_a_list() {
        String listElements = "(15 (\"abc\" #t) 7 #(1 2 3) (#\\u #f) 2 \"a_string\")";
        String input = "(quote " + listElements + ")";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is(listElements));
    }

    @Test
    public void multiple_quotation_is_possible() {
        String input = "(quote (quote (quote (1 2 3))))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("''(1 2 3)"));
    }

    @Test
    public void quoting_a_sequence_of_data_in_parentheses_and_prefixed_with_a_number_sign_produces_a_vector() {
        String listElements = "#(\"abc\" 1 #\\u 20 30 (#\\v #\\z #(1 2 3)))";
        String input = "(quote " + listElements + ")";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is(listElements));
    }

    @Test
    public void all_character_symbols_are_supported() {
        String characterSymbolsInList = "(+ - ... !.. $.+ %.- &.! *.: /:. :+. <-. =. >. ?. ~. _. ^.)";
        Assert.assertThat(visitParseTreeForInput("'" + characterSymbolsInList), Matchers.is(characterSymbolsInList));
    }

    @Test
    public void a_program_can_contain_multiple_expressions_on_the_top_level() {
        String input = "2 (define foo 3) \"bar\" foo #t (define baz #\\a) baz";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("2 \"bar\" 3 #t #\\a"));
    }

    @Test
    public void an_application_with_an_undefined_procedure_leads_to_an_error() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined procedure 'func'");

        visitParseTreeForInput("(func 1 2 3)");
    }

    @Test
    public void predefined_procedures_can_be_called() {
        String input = "(+ 2 3 (+ 10) (+ 3 7))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("25"));
        input = "(- 10 (- 5 200) 375 (- 20))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("-150"));
        input = "(* 2 (* 5) 10 (* 3 7))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("2100"));
        input = "(quotient 10 (quotient 7 3))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("5"));
    }

    @Test
    public void variables_can_be_referenced_in_procedure_call() {
        String input = "(define foo 2) (+ foo 1)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("3"));
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
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("1"));
        input = "(car '((\"abc\" \"xyz\") 5 (6 7) 8))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("(\"abc\" \"xyz\")"));
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
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("(2 3)"));
        input = "(cdr '((\"abc\" \"xyz\") 5 (6 7) 8))";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("(5 (6 7) 8)"));
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
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("42"));
    }

    @Test
    public void a_variable_can_be_referenced_in_the_body_of_a_procedure_definition() {
        String input = "(define foo 123) (define (bar) foo) (bar)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("123"));
    }

    @Test
    public void a_procedure_definition_can_contain_definitions() throws Exception {
        String input = "(define (foo) (define bar \"a_string\") bar) (foo)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("\"a_string\""));
    }

    @Test
    public void a_procedure_definition_can_contain_a_quotation_in_its_body() throws Exception {
        String input = "(define (foo) '(1 \"a_string\" (3 #t) #\\a)) (foo)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("(1 \"a_string\" (3 #t) #\\a)"));
    }

    @Test
    public void a_procedure_can_call_other_procedures_in_its_body() throws Exception {
        String input = "(define (double x) (* x 2)) (double 12)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("24"));
        input = "(define (square_and_add x y) (+ (* x x) (* y y))) (square_and_add 3 4)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("25"));
        input = "(define (add x y) (+ x y)) (define (add_1 x) (add x 1)) (add_1 6)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("7"));
    }

    @Test
    public void an_error_occurs_when_the_number_of_parameters_and_actual_arguments_does_not_match() throws Exception {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Expected 2 argument(s) but got 1 argument(s)");

        String input = "(define (add x y) (+ x y)) (add 12)";
        Assert.assertThat(visitParseTreeForInput(input), Matchers.is("25"));
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
        schemeParseTreeVisitor = new SchemeParseTreeVisitor();
        List<String> output = schemeParseTreeVisitor.visit(parseTree);

        return  String.join(" ", output);
    }

}

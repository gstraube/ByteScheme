import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import parser.ErrorListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
            assertThat(visitParseTreeForInput(constant), is(constant));
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
            assertThat(schemeParseTreeVisitor.variableDefinitions.get("a_variable").getText(), is(constant));
        }
    }

    @Test
    public void it_is_possible_to_reference_a_variable() {
        visitParseTreeForInput("(define a_variable 12) (define a_second_variable a_variable)");

        assertThat(schemeParseTreeVisitor.variableDefinitions.size(), is(2));
        assertThat(schemeParseTreeVisitor.variableDefinitions.get("a_variable").getText(), is("12"));
        assertThat(schemeParseTreeVisitor.variableDefinitions.get("a_second_variable").getText(), is("12"));
    }

    @Test
    public void trying_to_reference_an_undefined_variable_causes_an_exception() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined variable 'undefined_variable'");

        visitParseTreeForInput("(define a_variable undefined_variable)");
    }

    @Test
    public void quoting_a_constant_once_returns_the_constant() {
        for (String constant : CONSTANTS) {
            String result = visitParseTreeForInput(String.format("(quote %s)", constant));
            assertThat(result, is(constant));
            result = visitParseTreeForInput(String.format("'%s", constant));
            assertThat(result, is(constant));
        }
    }

    @Test
    public void quoting_an_identifier_produces_a_symbol() {
        assertThat(visitParseTreeForInput("(quote an_identifier)"), is("an_identifier"));
    }

    @Test
    public void quoting_a_sequence_of_data_in_parentheses_produces_a_list() {
        String listElements = "(15 (\"abc\" #t) 7 (#\\u #f) 2 \"a_string\")";
        String input = "(quote " + listElements + ")";
        assertThat(visitParseTreeForInput(input), is(listElements));
    }

    @Test
    public void multiple_quotation_is_possible() {
        String input = "(quote (quote (quote (1 2 3))))";
        assertThat(visitParseTreeForInput(input), is("''(1 2 3)"));
    }

    @Test
    public void all_character_symbols_are_supported() {
        String characterSymbolsInList = "(+ - ... !.. $.+ %.- &.! *.: /:. :+. <-. =. >. ?. ~. _. ^.)";
        assertThat(visitParseTreeForInput("'" + characterSymbolsInList), is(characterSymbolsInList));
    }

    @Test
    public void a_program_can_contain_multiple_expressions_on_the_top_level() {
        String input = "2 (define foo 3) \"bar\" foo #t (define baz #\\a) baz";
        assertThat(visitParseTreeForInput(input), is("2 \"bar\" 3 #t #\\a"));
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
        assertThat(visitParseTreeForInput(input), is("25"));
        input = "(- 10 (- 5 200) 375 (- 20))";
        assertThat(visitParseTreeForInput(input), is("-150"));
        input = "(* 2 (* 5) 10 (* 3 7))";
        assertThat(visitParseTreeForInput(input), is("2100"));
        input = "(quotient 10 (quotient 7 3))";
        assertThat(visitParseTreeForInput(input), is("5"));
    }

    @Test
    public void variables_can_be_referenced_in_procedure_call() {
        String input = "(define foo 2) (+ foo 1)";
        assertThat(visitParseTreeForInput(input), is("3"));
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
        assertThat(visitParseTreeForInput(input), is("1"));
        input = "(car '((\"abc\" \"xyz\") 5 (6 7) 8))";
        assertThat(visitParseTreeForInput(input), is("(\"abc\" \"xyz\")"));
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
        assertThat(visitParseTreeForInput(input), is("(2 3)"));
        input = "(cdr '((\"abc\" \"xyz\") 5 (6 7) 8))";
        assertThat(visitParseTreeForInput(input), is("(5 (6 7) 8)"));
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
        assertThat(visitParseTreeForInput(input), is("42"));
    }

    @Test
    public void a_variable_can_be_referenced_in_the_body_of_a_procedure_definition() {
        String input = "(define foo 123) (define (bar) foo) (bar)";
        assertThat(visitParseTreeForInput(input), is("123"));
    }

    @Test
    public void a_procedure_definition_can_contain_definitions() throws Exception {
        String input = "(define (foo) (define bar \"a_string\") bar) (foo)";
        assertThat(visitParseTreeForInput(input), is("\"a_string\""));
    }

    @Test
    public void a_procedure_definition_can_contain_a_quotation_in_its_body() throws Exception {
        String input = "(define (foo) '(1 \"a_string\" (3 #t) #\\a)) (foo)";
        assertThat(visitParseTreeForInput(input), is("(1 \"a_string\" (3 #t) #\\a)"));
    }

    @Test
    public void a_procedure_can_call_other_procedures_in_its_body() throws Exception {
        String input = "(define (double x) (* x 2)) (double 12)";
        assertThat(visitParseTreeForInput(input), is("24"));
        input = "(define (square_and_add x y) (+ (* x x) (* y y))) (square_and_add 3 4)";
        assertThat(visitParseTreeForInput(input), is("25"));
        input = "(define (add x y) (+ x y)) (define (add_1 x) (add x 1)) (add_1 6)";
        assertThat(visitParseTreeForInput(input), is("7"));
    }

    @Test
    public void an_error_occurs_when_the_number_of_parameters_and_actual_arguments_does_not_match() throws Exception {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Expected 2 argument(s) but got 1 argument(s)");

        String input = "(define (add x y) (+ x y)) (add 12)";
        assertThat(visitParseTreeForInput(input), is("25"));
    }

    @Test
    public void equal_returns_false_when_types_do_not_match() throws Exception {
        String input = "(equal? 42 \"forty-two\")";
        assertThat(visitParseTreeForInput(input), is("#f"));
        input = "(equal? '(1 2 3) #t)";
        assertThat(visitParseTreeForInput(input), is("#f"));
    }

    @Test
    public void equal_returns_true_for_constants_with_matching_values_and_false_otherwise() {
        String input = "(equal? 42 42)";
        assertThat(visitParseTreeForInput(input), is("#t"));
        input = "(equal? #\\a #\\a)";
        assertThat(visitParseTreeForInput(input), is("#t"));
        input = "(equal? \"a string\" \"a different string\")";
        assertThat(visitParseTreeForInput(input), is("#f"));
        input = "(equal? #t #f)";
        assertThat(visitParseTreeForInput(input), is("#f"));
    }

    @Test
    public void equal_returns_true_for_two_lists_containing_the_same_elements_and_false_otherwise() throws Exception {
        String input = "(equal? '(1 \"foo\" 2 (#t 4) (5 6)) '(1 \"foo\" 2 (#t 4) (5 6)))";
        assertThat(visitParseTreeForInput(input), is("#t"));
        input = "(equal? '(1 2 3) '(4 5))";
        assertThat(visitParseTreeForInput(input), is("#f"));
        input = "(equal? '(1 2 3) '())";
        assertThat(visitParseTreeForInput(input), is("#f"));
        input = "(equal? '(1 2 3) '(1 2 #t))";
        assertThat(visitParseTreeForInput(input), is("#f"));
    }

    @Test
    public void the_if_statement_is_evaluated_correctly() {
        String input = "(if (equal? 42 42) \"equal\" \"not equal\")";
        assertThat(visitParseTreeForInput(input), is("\"equal\""));
        input = "(if (equal? 42 25) \"equal\" \"not equal\")";
        assertThat(visitParseTreeForInput(input), is("\"not equal\""));
        input = "(if (equal? 42 \"foo\") \"equal\" \"not equal\")";
        assertThat(visitParseTreeForInput(input), is("\"not equal\""));
    }

    @Test
    public void comparison_operators_are_evaluated_correctly() throws Exception {
        Map<String, String> expectedResults = new HashMap() {{
            put("(< 5 10)", "#t");
            put("(< 10 5)", "#f");
            put("(< 10 10)", "#f");
            put("(> 10 5)", "#t");
            put("(> 5 10)", "#f");
            put("(> 10 10)", "#f");
            put("(<= 5 10)", "#t");
            put("(<= 10 5)", "#f");
            put("(<= 10 10)", "#t");
            put("(>= 10 5)", "#t");
            put("(>= 5 10)", "#f");
            put("(>= 10 10)", "#t");
        }};
        for (String input : expectedResults.keySet()) {
            assertThat(visitParseTreeForInput(input), is(expectedResults.get(input)));
        }
    }

    @Test
    public void it_is_possible_to_define_recursive_procedures() throws Exception {
        String input = "(define (fib n) (if (< n 3) 1 (+ (fib (- n 1)) (fib (- n 2)))))";
        input += "(fib 10)";
        assertThat(visitParseTreeForInput(input), is("55"));
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

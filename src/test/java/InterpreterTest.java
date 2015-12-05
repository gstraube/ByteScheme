import javassist.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import parser.ErrorListener;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InterpreterTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    CodeGenVisitor codeGenVisitor;

    public static final String[] CONSTANTS = new String[]{
            "1", "42", "-1237", "#t",
            "#\\λ", "#\\newline", "#\\space", "\"a string\""
    };

    private CtClass mainClassCt;

    private static AtomicInteger classIndex = new AtomicInteger(0);

    @Before
    public void setup() {
        ClassPool pool = ClassPool.getDefault();
        pool.importPackage("runtime");
        mainClassCt = pool.makeClass(String.format("Main%d", classIndex.getAndIncrement()));
    }

    @Test
    public void constant_expressions_are_interpreted_correctly() {
        String input = stream(CONSTANTS).collect(Collectors.joining(" "));

        List<String> interpret = interpret(input);
        assertThat(interpret.get(0), is("1"));
        assertThat(interpret.get(1), is("42"));
        assertThat(interpret.get(2), is("-1237"));
        assertThat(interpret.get(3), is("#t"));
        assertThat(interpret.get(4), is("#\\λ"));
        assertThat(interpret.get(5), is("#\\newline"));
        assertThat(interpret.get(6), is("#\\space"));
        assertThat(interpret.get(7), is("a string"));
    }

    @Test
    public void variable_expressions_evaluate_to_the_correct_value() {
        Stream<String> definitions = IntStream.range(0, CONSTANTS.length)
                .parallel()
                .mapToObj(index -> String.format("(define a_variable%d %s) a_variable%d",
                        index, CONSTANTS[index], index));
        String input = definitions.collect(Collectors.joining(" "));

        List<String> interpret = interpret(input);
        assertThat(interpret.get(0), is("1"));
        assertThat(interpret.get(1), is("42"));
        assertThat(interpret.get(2), is("-1237"));
        assertThat(interpret.get(3), is("#t"));
        assertThat(interpret.get(4), is("#\\λ"));
        assertThat(interpret.get(5), is("#\\newline"));
        assertThat(interpret.get(6), is("#\\space"));
        assertThat(interpret.get(7), is("a string"));
    }

    @Test
    public void quotation_marks_are_not_allowed_in_string_constants() {
        expectedException.expect(ParseCancellationException.class);

        interpret("\"\"\"");
    }

    @Test
    public void backslashes_are_not_allowed_in_string_constants() {
        expectedException.expect(ParseCancellationException.class);

        interpret("\"\\\"");
    }

    @Test
    public void trying_to_reference_an_undefined_variable_causes_an_exception() {
        expectedException.expect(ParseCancellationException.class);
        expectedException.expectMessage("Undefined variable 'undefined_variable'");

        visitParseTreeForInput("(define a_variable undefined_variable)");
    }

    private List<String> interpret(String input) {
        GeneratedCode generatedCode = visitParseTreeForInput(input);
        Class<?> mainClass;
        List<String> outputs = new ArrayList<>();

        try {
            for (String variableDefinition : generatedCode.getVariableDefinitions()) {
                String escapedDefinition = variableDefinition.replace("\n", "\\n");
                mainClassCt.addField(CtField.make(escapedDefinition, mainClassCt));
            }

            for (String method : generatedCode.getMethodsToBeDeclared()) {
                String escapedMethod = method.replace("\n", "\\n");
                mainClassCt.addMethod(CtMethod.make(escapedMethod, mainClassCt));
            }

            mainClass = mainClassCt.toClass();
            Object mainInstance = mainClass.newInstance();

            for (String method : generatedCode.getMethodsToBeCalled()) {
                outputs.add((String) mainClass.getMethod(method).invoke(mainInstance));
            }

        } catch (CannotCompileException | InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        mainClassCt.defrost();

        return outputs;
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

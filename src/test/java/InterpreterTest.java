import javassist.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import parser.ErrorListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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

    public static final String[] EXPECTED_OUTPUT = new String[]{
            "1", "42", "-1237", "#t",
            "λ", "\n", " ", "a string"
    };

    private CtClass mainClassCt;

    private static AtomicInteger classIndex = new AtomicInteger(0);

    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream systemOut = System.out;

    @Before
    public void setup() {
        ClassPool pool = ClassPool.getDefault();
        pool.importPackage("runtime");
        mainClassCt = pool.makeClass(String.format("Main%d", classIndex.getAndIncrement()));

        System.setOut(new PrintStream(out));
    }

    @Test
    public void constant_expressions_are_interpreted_correctly() {
        String input = stream(CONSTANTS)
                .map(constant -> String.format("(display %s)", constant))
                .collect(Collectors.joining(" "));

        String expectedOutput = stream(EXPECTED_OUTPUT)
                .collect(Collectors.joining("\n"));
        expectedOutput += "\n";

        interpret(input);

        assertThat(out.toString(), is(expectedOutput));
    }

    @Test
    public void variable_expressions_evaluate_to_the_correct_value() {
        Stream<String> definitions = IntStream.range(0, CONSTANTS.length)
                .parallel()
                .mapToObj(index -> String.format("(define a_variable%d %s) (display a_variable%d)",
                        index, CONSTANTS[index], index));
        String input = definitions.collect(Collectors.joining(" "));

        String expectedOutput = stream(EXPECTED_OUTPUT)
                .collect(Collectors.joining("\n"));
        expectedOutput += "\n";

        interpret(input);

        assertThat(out.toString(), is(expectedOutput));
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

    @After
    public void tearDown() {
        System.setOut(systemOut);
    }

    private List<String> interpret(String input) {
        GeneratedCode generatedCode = visitParseTreeForInput(input);
        Class<?> mainClass;
        List<String> outputs = new ArrayList<>();

        try {
            mainClassCt.addConstructor(CtNewConstructor.defaultConstructor(mainClassCt));

            for (String variableDefinition : generatedCode.getVariableDefinitions()) {
                String escapedDefinition = variableDefinition.replace("\n", "\\n");
                mainClassCt.addField(CtField.make(escapedDefinition, mainClassCt));
            }

            for (String method : generatedCode.getMethodsToBeDeclared()) {
                String escapedMethod = method.replace("\n", "\\n");
                mainClassCt.addMethod(CtMethod.make(escapedMethod, mainClassCt));
            }

            mainClass = mainClassCt.toClass();

            String[] emptyArgs = new String[0];
            mainClass.getMethod("main", emptyArgs.getClass()).invoke(null, (Object) emptyArgs);

        } catch (CannotCompileException | IllegalAccessException |
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

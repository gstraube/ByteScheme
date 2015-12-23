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
import org.junit.rules.TemporaryFolder;
import parser.ErrorListener;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static java.util.Arrays.stream;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InterpreterTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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
    private ClassPool pool;

    @Before
    public void setup() {
        pool = ClassPool.getDefault();
        pool.importPackage("runtime");
        mainClassCt = pool.makeClass(String.format("Main%d", classIndex.get()));

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

        String output = interpret(input);

        assertThat(output, is(expectedOutput));
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

        String output = interpret(input);

        assertThat(output, is(expectedOutput));
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

    private String interpret(String input) {
        GeneratedCode generatedCode = visitParseTreeForInput(input);

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

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,
                    String.format("Main%d", classIndex.get()));
            File file = folder.newFile("output.jar");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            JarOutputStream jarOut = new JarOutputStream(fileOutputStream, manifest);
            jarOut.putNextEntry(new ZipEntry(String.format("Main%d.class", classIndex.getAndIncrement())));
            jarOut.write(mainClassCt.toBytecode());
            jarOut.closeEntry();
            jarOut.putNextEntry(new ZipEntry("runtime/OutputFormatter.class"));
            jarOut.write(pool.get("runtime.OutputFormatter").toBytecode());
            jarOut.closeEntry();
            jarOut.close();
            fileOutputStream.close();

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(folder.getRoot());
            processBuilder.command("java", "-jar", "output.jar");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }

            return builder.toString();
        } catch (CannotCompileException | IOException | NotFoundException e) {
            e.printStackTrace();
        }

        mainClassCt.defrost();

        return "";
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

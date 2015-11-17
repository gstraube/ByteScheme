import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Before;
import org.junit.Test;
import parser.ErrorListener;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InterpreterTest {

    CodeGenVisitor codeGenVisitor;

    public static final String[] CONSTANTS = new String[]{
            "1", "42", "-1237", "#t",
            "#\\λ", "#\\newline", "#\\space", "\"a string\""
    };
    private CtClass mainClassCt;

    @Before
    public void setup() {
        ClassPool pool = ClassPool.getDefault();
        mainClassCt = pool.makeClass("Main");
    }

    @Test
    public void constant_expressions_are_interpreted_correctly() {
        String input = stream(CONSTANTS).collect(Collectors.joining(" "));

        List<String> interpret = interpret(input);
        assertThat(interpret.get(0), is("1"));
        assertThat(interpret.get(1), is("42"));
        assertThat(interpret.get(2), is("-1237"));
        assertThat(interpret.get(3), is("true"));
        assertThat(interpret.get(4), is("λ"));
        assertThat(interpret.get(5), is("\n"));
        assertThat(interpret.get(6), is(" "));
        assertThat(interpret.get(7), is("a string"));
    }

    private List<String> interpret(String input) {
        GeneratedCode generatedCode = visitParseTreeForInput(input);
        Class<?> mainClass;
        List<String> outputs = new ArrayList<>();

        try {
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

        mainClassCt.detach();

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
        return codeGenVisitor.visit(parseTree);
    }

}

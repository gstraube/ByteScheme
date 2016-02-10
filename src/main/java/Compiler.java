import javassist.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import parser.ErrorListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Compiler {

    CodeGenVisitor codeGenVisitor;
    private CtClass mainClassCt;
    private ClassPool pool;
    private File jarFile;

    public Compiler(File jarFile) {
        this.jarFile = jarFile;
        pool = ClassPool.getDefault();
        pool.importPackage("runtime");
        pool.importPackage("lang");
        pool.importPackage("java.math.BigInteger");
        mainClassCt = pool.makeClass("Main");
    }

    public void compile(String input) {
        GeneratedCode generatedCode = visitParseTreeForInput(input);

        try {
            createMainClassCt(generatedCode);

            createJarFile();
        } catch (CannotCompileException | IOException | NotFoundException e) {
            e.printStackTrace();
        }

        mainClassCt.defrost();
    }

    private void createMainClassCt(GeneratedCode generatedCode) throws CannotCompileException {
        mainClassCt.addConstructor(CtNewConstructor.defaultConstructor(mainClassCt));

        for (String variableDefinition : generatedCode.getVariableDefinitions()) {
            String escapedDefinition = variableDefinition.replace("\n", "\\n");
            mainClassCt.addField(CtField.make(escapedDefinition, mainClassCt));
        }

        for (String method : generatedCode.getMethodsToBeDeclared()) {
            String escapedMethod = method.replace("\n", "\\n");
            mainClassCt.addMethod(CtMethod.make(escapedMethod, mainClassCt));
        }
    }

    private void createJarFile() throws IOException, CannotCompileException, NotFoundException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "Main");
        FileOutputStream fileOutputStream = new FileOutputStream(jarFile);
        JarOutputStream jarOut = new JarOutputStream(fileOutputStream, manifest);
        addEntryToJar(jarOut, "Main.class", mainClassCt.toBytecode());
        addEntryToJar(jarOut, "runtime/OutputFormatter.class", pool.get("runtime.OutputFormatter").toBytecode());
        addEntryToJar(jarOut, "runtime/PredefinedProcedures.class",
                pool.get("runtime.PredefinedProcedures").toBytecode());
        addEntryToJar(jarOut, "lang/ListWrapper.class", pool.get("lang.ListWrapper").toBytecode());
        jarOut.close();
        fileOutputStream.close();
    }

    private void addEntryToJar(JarOutputStream jarOut, String entryPath, byte[] byteCode)
            throws IOException, CannotCompileException, NotFoundException {
        jarOut.putNextEntry(new ZipEntry(entryPath));
        jarOut.write(byteCode);
        jarOut.closeEntry();
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
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeneratedCodeBuilderTest {

    @Test
    public void a_class_with_no_fields_and_methods_has_only_an_empty_main_method() {
        GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();

        GeneratedCode generatedCode = generatedCodeBuilder.build();
        assertThat(generatedCode.getVariableDefinitions().size(), is(0));
        assertThat(generatedCode.getMethodsToBeDeclared().size(), is(1));
        assertThat(generatedCode.getMethodsToBeDeclared().get(0), is("public static void main(String[] args){}"));
    }

    @Test
    public void a_class_can_contain_fields_and_methods() {
        GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();

        GeneratedCode generatedCode = generatedCodeBuilder
                .addVariableDefinitions("String aString = \"a string\";", "boolean a = false;")
                .addMethodsToBeDeclared("public String someMethod(){return \"a value\"}")
                .build();

        assertThat(generatedCode.getVariableDefinitions().size(), is(2));
        assertThat(generatedCode.getVariableDefinitions().get(0), is("String aString = \"a string\";"));
        assertThat(generatedCode.getVariableDefinitions().get(1), is("boolean a = false;"));

        assertThat(generatedCode.getMethodsToBeDeclared().size(), is(2));
        assertThat(generatedCode.getMethodsToBeDeclared().get(0),
                is("public String someMethod(){return \"a value\"}"));
        assertThat(generatedCode.getMethodsToBeDeclared().get(1),
                is("public static void main(String[] args){}"));
    }

    @Test
    public void it_is_possible_to_add_statements_to_the_main_method() {
        GeneratedCode.GeneratedCodeBuilder generatedCodeBuilder = new GeneratedCode.GeneratedCodeBuilder();

        GeneratedCode generatedCode = generatedCodeBuilder
                .addStatementsToMainMethod("String var = \"a string\";",
                        "System.out.println(OutputFormatter.output(var));")
                .build();

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("public static void main(String[] args){");
        expectedOutput.append("String var = \"a string\";");
        expectedOutput.append("System.out.println(OutputFormatter.output(var));");
        expectedOutput.append("}");

        assertThat(generatedCode.getMethodsToBeDeclared().get(0), is(expectedOutput.toString()));
    }

}
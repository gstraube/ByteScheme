import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GeneratedCode {

    private List<String> methodsToBeDeclared = new ArrayList<>();

    private List<String> methodsToBeCalled = new ArrayList<>();

    private List<String> constants = new ArrayList<>();
    private List<String> variableDefinitions = new ArrayList<>();

    public GeneratedCode(List<String> methodsToBeDeclared, List<String> variableDefinitions, List<String> constants) {
        this.methodsToBeDeclared = methodsToBeDeclared;
        this.variableDefinitions = variableDefinitions;
        this.constants = constants;
    }

    public List<String> getMethodsToBeDeclared() {
        return new ArrayList<>(methodsToBeDeclared);
    }

    public List<String> getMethodsToBeCalled() {
        return new ArrayList<>(methodsToBeCalled);
    }

    public List<String> getVariableDefinitions() {
        return new ArrayList<>(variableDefinitions);
    }

    public List<String> getConstants() {
        return new ArrayList<>(constants);
    }

    public static class GeneratedCodeBuilder {

        private List<String> mainMethod = new ArrayList<>();
        private List<String> methodsToBeDeclared = new ArrayList<>();
        private List<String> variableDefinitions = new ArrayList<>();
        private List<String> constants = new ArrayList<>();

        public GeneratedCodeBuilder addVariableDefinitions(String... variableDefinitions) {
            this.variableDefinitions.addAll(Arrays.asList(variableDefinitions));
            return this;
        }

        public GeneratedCodeBuilder addMethodsToBeDeclared(String... methodsToBeDeclared) {
            this.methodsToBeDeclared.addAll(Arrays.asList(methodsToBeDeclared));
            return this;
        }

        public GeneratedCode build() {
            mainMethod.add(0, "public static void main(String[] args){");
            mainMethod.add("}");

            methodsToBeDeclared.add(mainMethod.stream().collect(Collectors.joining()));

            return new GeneratedCode(methodsToBeDeclared, variableDefinitions, constants);
        }

        public GeneratedCodeBuilder addConstant(String constant) {
            constants.add(constant);

            return this;
        }

        public GeneratedCodeBuilder addVariableDefinition(String variable) {
            variableDefinitions.add(variable);

            return this;
        }

        public String getConstant(int index) {
            return constants.get(index);
        }

        public GeneratedCodeBuilder mergeWith(GeneratedCodeBuilder other) {
            GeneratedCodeBuilder merged = new GeneratedCodeBuilder();

            merged.constants.addAll(constants);
            merged.constants.addAll(other.constants);

            merged.variableDefinitions.addAll(variableDefinitions);
            merged.variableDefinitions.addAll(other.variableDefinitions);

            merged.methodsToBeDeclared.addAll(methodsToBeDeclared);
            merged.methodsToBeDeclared.addAll(other.methodsToBeDeclared);

            return merged;
        }

        public GeneratedCodeBuilder addStatementsToMainMethod(String... statements) {
            mainMethod.addAll(Arrays.asList(statements));

            return this;
        }
    }

}

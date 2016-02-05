import java.util.*;
import java.util.stream.Collectors;

public class GeneratedCode {

    private List<String> methodsToBeDeclared = new ArrayList<>();

    private List<String> variableDefinitions = new ArrayList<>();
    private String generatedCode;

    public GeneratedCode(List<String> methodsToBeDeclared, List<String> variableDefinitions, String generatedCode) {
        this.methodsToBeDeclared = methodsToBeDeclared;
        this.variableDefinitions = variableDefinitions;
        this.generatedCode = generatedCode;
    }

    public List<String> getMethodsToBeDeclared() {
        return new ArrayList<>(methodsToBeDeclared);
    }

    public List<String> getVariableDefinitions() {
        return new ArrayList<>(variableDefinitions);
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public static class GeneratedCodeBuilder {

        private List<String> mainMethod = new ArrayList<>();
        private Map<String, Optional<String>> methodsToBeDeclared = new LinkedHashMap<>();
        private List<String> variableDefinitions = new ArrayList<>();
        private String generatedCode;

        public GeneratedCodeBuilder addVariableDefinitions(String... variableDefinitions) {
            this.variableDefinitions.addAll(Arrays.asList(variableDefinitions));
            return this;
        }

        public GeneratedCode build() {
            mainMethod.add(0, "public static void main(String[] args){");
            mainMethod.add("}");

            List<String> methods = new ArrayList<>();

            methodsToBeDeclared.values()
                    .stream()
                    .filter(Optional::isPresent)
                    .forEach(method -> methods.add(method.get()));
            methods.add(mainMethod.stream().collect(Collectors.joining()));

            return new GeneratedCode(methods, variableDefinitions, generatedCode);
        }

        public GeneratedCodeBuilder setGeneratedCode(String generatedCode) {
            this.generatedCode = generatedCode;

            return this;
        }

        public GeneratedCodeBuilder addVariableDefinition(String variable) {
            variableDefinitions.add(variable);

            return this;
        }

        public String getGeneratedCode() {
            return generatedCode;
        }

        public GeneratedCodeBuilder mergeWith(GeneratedCodeBuilder other) {
            GeneratedCodeBuilder merged = new GeneratedCodeBuilder();

            merged.generatedCode = generatedCode != null ? generatedCode : other.generatedCode;

            merged.variableDefinitions.addAll(variableDefinitions);
            merged.variableDefinitions.addAll(other.variableDefinitions);

            merged.methodsToBeDeclared.putAll(methodsToBeDeclared);
            merged.methodsToBeDeclared.putAll(other.methodsToBeDeclared);

            merged.mainMethod.addAll(mainMethod);
            merged.mainMethod.addAll(other.mainMethod);

            return merged;
        }

        public GeneratedCodeBuilder addStatementsToMainMethod(String... statements) {
            mainMethod.addAll(Arrays.asList(statements));

            return this;
        }

        public GeneratedCodeBuilder addMethodToBeDeclared(String generatedMethod) {
            methodsToBeDeclared.put(generatedMethod, Optional.empty());

            return this;
        }

        public GeneratedCodeBuilder addMethodToBeDeclared(String procedureName, String generatedMethod) {
            methodsToBeDeclared.put(procedureName, Optional.of(generatedMethod));

            return this;
        }

    }

}

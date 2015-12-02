import java.util.ArrayList;
import java.util.List;

public class GeneratedCode {

    private List<String> methodsToBeDeclared = new ArrayList<>();

    private List<String> methodsToBeCalled = new ArrayList<>();

    private List<String> constants = new ArrayList<>();
    private List<String> variableDefinitions = new ArrayList<>();

    private GeneratedCode() {
    }

    public static GeneratedCode empty() {
        return new GeneratedCode();
    }

    public void addMethodToBeDeclared(String method) {
        methodsToBeDeclared.add(method);
    }

    public void addMethodToBeCalled(String method) {
        methodsToBeCalled.add(method);
    }

    public void addConstant(String constant) {
        constants.add(constant);
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

    public String getConstant(int index) {
        return constants.get(index);
    }

    public GeneratedCode mergeWith(GeneratedCode nextResult) {
        GeneratedCode merged = GeneratedCode.empty();

        merged.methodsToBeCalled.addAll(this.methodsToBeCalled);
        merged.methodsToBeCalled.addAll(nextResult.methodsToBeCalled);

        merged.methodsToBeDeclared.addAll(this.methodsToBeDeclared);
        merged.methodsToBeDeclared.addAll(nextResult.methodsToBeDeclared);

        merged.variableDefinitions.addAll(this.variableDefinitions);
        merged.variableDefinitions.addAll(nextResult.variableDefinitions);

        merged.constants.addAll(this.constants);
        merged.constants.addAll(nextResult.constants);

        return merged;
    }

    public void addVariableDefinition(String variableDefinition) {
        variableDefinitions.add(variableDefinition);
    }

}

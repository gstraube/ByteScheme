import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ByteScheme {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Received wrong number of arguments");
            System.exit(1);
        }

        String sourceFileName = args[0];

        Path path = Paths.get(sourceFileName);
        byte[] inputByteContent = Files.readAllBytes(path);
        String sourceInput = new String(inputByteContent, "utf8");

        String outputFileName = path.getFileName() + ".jar";
        File outputFile = new File(outputFileName);

        Compiler compiler = new Compiler(outputFile);
        compiler.compile(sourceInput);
    }

}

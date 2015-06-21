import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import parser.ErrorListener;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

public class ReadEvalPrintLoop {


    private static SchemeParseTreeVisitor schemeParseTreeVisitor = new SchemeParseTreeVisitor();
    private static PrintStream outputStream = System.out;

    private static final String WELCOME_MESSAGE = "Welcome to ByteScheme. Enter \"exit\" to quit.";
    private static final String EXIT_MESSAGE = "Bye!";
    private static final String EXIT_KEYWORD = "exit";
    private static final String PROMPT_SYMBOL = ">";

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);

        printLine(WELCOME_MESSAGE);
        printPromptSymbol();
        while (inputScanner.hasNextLine()) {

            String line = inputScanner.nextLine();

            if (line.equalsIgnoreCase(EXIT_KEYWORD)) {
                printLine(EXIT_MESSAGE);
                System.exit(0);
            }

            String output;
            try {
                output = visitParseTreeForInput(line);
            } catch (ParseCancellationException exception) {
                output = exception.getMessage();
            }

            if (!output.equals("")) {
                printLine(output);
            }
            printPromptSymbol();
        }

    }

    private static void printLine(String line) {
        outputStream.println(line);
    }

    private static void printPromptSymbol() {
        System.out.print(PROMPT_SYMBOL);
    }

    private static String visitParseTreeForInput(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        SchemeLexer lexer = new SchemeLexer(inputStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SchemeParser parser = new SchemeParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ErrorListener.INSTANCE);

        ParseTree parseTree = parser.program();
        List<String> output = schemeParseTreeVisitor.visit(parseTree);

        return String.join(" ", output);
    }

}

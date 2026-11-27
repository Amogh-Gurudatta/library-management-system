import java.util.Scanner;

/** Entry point: reads commands from standard input until it ends. */
public class Main {
    public static void main(String[] args) {
        new CommandProcessor(new Library()).run(new Scanner(System.in), System.out);
    }
}

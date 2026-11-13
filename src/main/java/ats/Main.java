package ats;

import ats.cli.ConsoleUI;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Starts the command-line application with an empty, in-memory clinic. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        new ConsoleUI(new Clinic(), System.in, out).run();
    }
}

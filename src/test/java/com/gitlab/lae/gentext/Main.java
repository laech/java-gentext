package com.gitlab.lae.gentext;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.UTF_8;

class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Expected single file path argument.");
            System.exit(0);
        }

        System.out.println("Loading " + args[0]);
        long start = System.currentTimeMillis();
        TextGenerator gen = load(Paths.get(args[0]));
        long end = System.currentTimeMillis();
        System.out.printf("Took %,d ms%n", end - start);

        Scanner scanner = new Scanner(System.in, "UTF-8");
        while (true) {
            String phrase = scanner.nextLine();
            int n = ThreadLocalRandom.current().nextInt(10, 30);
            System.err.println(phrase + " " + gen.generate(ThreadLocalRandom.current(), n));
        }
    }

    private static TextGenerator load(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, UTF_8)) {
            return TextGenerator.create(reader, 2);
        }
    }
}

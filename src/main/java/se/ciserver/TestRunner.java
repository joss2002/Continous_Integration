package se.ciserver;

import java.io.*;
import java.util.function.Consumer;

public class TestRunner {

    public static Consumer<String[]> commandHook = null;
    /**
     * Executes automated test suite for the specified git branch
     * 
     * @param branch name of git branch on which the tests should run
     * @return string containing maven logs
     * @throws Exception possible I/O exception
     */
    public static String runTests(String branch) throws Exception {
        // Checkout the correct branch dynamically
        runCommand("git", "checkout", branch);
        runCommand("git", "pull");

        if (commandHook != null) {
            commandHook.accept(new String[]{"mvn", "-B", "-Dtest=MainTest", "test"});
            return "Hooked: skipped Maven execution";
        }

        String mvnCommand = "mvn";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            mvnCommand = "mvn.cmd";
        }

        ProcessBuilder pb = new ProcessBuilder(mvnCommand,"-B","-Dtest=MainTest", "test");

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
            System.out.println(line);  
        }

        int exitCondition = process.waitFor();

        if(exitCondition == 0) { // Normal termination
            return "All test passed \n" + sb;
        } else {
            return "Tests failed \n" + sb;
        }
    }

    private static void runCommand(String... command) throws Exception {
        if (commandHook != null) {
            commandHook.accept(command); // TEST MODE
            return;
        }


        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();
    }
}
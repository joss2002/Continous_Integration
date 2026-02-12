package se.ciserver.build;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Handles cloning a repository, checking out a specific commit,
 * and running Maven compilation.
 */
public class Compiler
{
    /**
     * Clones the repository, checks out the specified commit, and
     * runs {@code mvn clean compile}.
     *
     * @param cloneUrl  The clone URL of the repository
     * @param branch    The branch name to clone
     * @param commitSha The commit SHA to checkout
     *
     * @return A {@link CompilationResult} indicating success/failure and build output
     */
    public CompilationResult compile(String cloneUrl, String branch, String commitSha)
    {
        Path tempDir = null;

        try
        {
            // Create an isolated temporary directory for this build
            tempDir = Files.createTempDirectory("ci-build-");

            // Step 1: Clone only the target branch (--single-branch avoids
            // downloading the full repo history)
            int cloneExit = runProcess(tempDir.getParent(),
                "git", "clone", "--branch", branch, "--single-branch",
                cloneUrl, tempDir.toString());

            if (cloneExit != 0)
            {
                return new CompilationResult(false,
                    "Git clone failed with exit code " + cloneExit);
            }

            // Step 2: Checkout the exact commit SHA that triggered the webhook
            int checkoutExit = runProcess(tempDir,
                "git", "checkout", commitSha);

            if (checkoutExit != 0)
            {
                return new CompilationResult(false,
                    "Git checkout failed with exit code " + checkoutExit);
            }

            // Step 3: Run Maven compilation and return the result
            return runCompilation(tempDir);
        }
        catch (IOException | InterruptedException e)
        {
            return new CompilationResult(false,
                "Compilation error: " + e.getMessage());
        }
        finally
        {
            // Always clean up the temporary directory to avoid disk bloat
            if (tempDir != null)
            {
                cleanup(tempDir);
            }
        }
    }

    /**
     * Runs a process and returns its exit code.
     * Output is printed to System.out for server console visibility.
     *
     * @param workDir The working directory for the process
     * @param command The command and its arguments
     *
     * @return The process exit code
     *
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    private int runProcess(Path workDir, String... command)
            throws IOException, InterruptedException
    {
        ProcessBuilder pb = createProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true); // Merge stderr into stdout

        Process process = pb.start();

        // Consume output line-by-line and print to the server console
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                System.out.println(line);
            }
        }

        // Block until the process finishes and return its exit code
        return process.waitFor();
    }

    /**
     * Runs {@code mvn clean compile} in the given directory and captures output.
     *
     * @param workDir The directory containing the Maven project
     *
     * @return A {@link CompilationResult} with the build outcome
     *
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    private CompilationResult runCompilation(Path workDir)
            throws IOException, InterruptedException
    {
        ProcessBuilder pb = createProcessBuilder("mvn", "clean", "compile");
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true); // Merge stderr into stdout

        Process process = pb.start();

        // Capture all build output into a single string
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())))
        {
            output = reader.lines()
                .collect(Collectors.joining(System.lineSeparator()));
        }

        int exitCode = process.waitFor();

        // Print Maven output to server console so grader can observe the build
        System.out.println(output);

        // Exit code 0 means compilation succeeded
        return new CompilationResult(exitCode == 0, output);
    }

    /**
     * Creates a ProcessBuilder for the given command.
     * Protected so tests can override to avoid spawning real processes.
     *
     * @param command The command and its arguments
     *
     * @return A new ProcessBuilder configured for the command
     */
    protected ProcessBuilder createProcessBuilder(String... command)
    {
        return new ProcessBuilder(command);
    }

    /**
     * Recursively deletes the temporary build directory.
     *
     * @param directory The directory to delete
     */
    private void cleanup(Path directory)
    {
        try
        {
            Files.walk(directory)
                 .sorted(Comparator.reverseOrder())
                 .forEach(path ->
                 {
                     try
                     {
                         Files.delete(path);
                     }
                     catch (IOException e)
                     {
                         /* best effort cleanup */
                     }
                 });
        }
        catch (IOException e)
        {
            System.err.println("Warning: Failed to clean up " + directory);
        }
    }
}

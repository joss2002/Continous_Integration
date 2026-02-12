package se.ciserver.build;

/**
 * Holds the result of a compilation attempt.
 */
public class CompilationResult
{
    /** Whether the compilation succeeded (exit code 0). */
    public final boolean success;
    public final boolean testSuccess;

    /** The combined stdout/stderr output from the build. */
    public final String output;
    /** The combined stdout/stderr output from the tests. */
    public final String testOutput;

    /**
     * Constructs a CompilationResult.
     *
     * @param success Whether the compilation succeeded
     * @param testSuccess Whether all the tests succeeded
     * @param output  The combined stdout/stderr output from the build
     * @param testOutput  The combined stdout/stderr output from the tests
     */
    public CompilationResult(boolean success, boolean testSuccess, String output, String testOutput)
    {
        this.success = success;
        this.output  = output;
        this.testSuccess = testSuccess;
        this.testOutput = testOutput;
    }
}

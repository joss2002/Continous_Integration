package se.ciserver.build;

/**
 * Holds the result of a compilation attempt.
 */
public class CompilationResult
{
    /** Whether the compilation succeeded (exit code 0). */
    public final boolean success;

    /** The combined stdout/stderr output from the build. */
    public final String output;

    /**
     * Constructs a CompilationResult.
     *
     * @param success Whether the compilation succeeded
     * @param output  The combined stdout/stderr output from the build
     */
    public CompilationResult(boolean success, String output)
    {
        this.success = success;
        this.output  = output;
    }
}

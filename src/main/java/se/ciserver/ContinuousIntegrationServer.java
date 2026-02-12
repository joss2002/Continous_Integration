package se.ciserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.stream.Collectors;

import se.ciserver.build.Compiler;
import se.ciserver.build.CompilationResult;
import se.ciserver.github.Push;
import se.ciserver.github.PushParser;
import se.ciserver.github.InvalidPayloadException;

/**
 * A Jetty-based CI-server that can start locally and receive HTTP-requests.
 */
public class ContinuousIntegrationServer extends AbstractHandler
{
    /** Flag to skip test execution during integration tests. */
    public static boolean isIntegrationTest = false;

    private final PushParser parser   = new PushParser();
    private final Compiler   compiler = new Compiler();

    /**
     * Handles incoming HTTP requests for the CI server and presents necessary information.
     *
     * @param target      - The requested URL
     * @param baseRequest - Jetty-specific request object, used to mark the request as handled
     * @param request     - Standard Java Servlet request
     * @param response    - Standard Java Servlet response
     *
     * @throws IOException      If an input/output error occurs when reading the request or writing the response
     * @throws ServletException If an internal Jetty/Servlet error occurs while handling the request
     */
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException
    {
        if ("/webhook".equals(target) && "POST".equalsIgnoreCase(request.getMethod()))
        {
            // Read the full JSON payload from the request body
            String json = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            try
            {
                // Parse the GitHub push event payload into a Push object
                Push push = parser.parse(json);

                // Log the push event details to the server console
                System.out.println("\nReceived push on branch : " + push.ref +
                                   "\nAfter SHA               : " + push.after +
                                   "\nRepository URL          : " + push.repository.clone_url +
                                   "\nPusher name             : " + push.pusher.name +
                                   "\n\nHead commit message     : " + push.head_commit.message);

                // P1: Clone the pushed branch and run mvn clean compile
                System.out.println("\nStarting compilation...");
                CompilationResult result = compiler.compile(
                    push.repository.clone_url,
                    push.ref,
                    push.after);

                // Log the compilation outcome to the server console
                if (result.success)
                {
                    System.out.println("\nCompilation SUCCEEDED");
                }
                else
                {
                    System.out.println("\nCompilation FAILED");
                }

                // Respond with 200 regardless of build outcome;
                // the webhook delivery itself was successful
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Push received: " + push.after);

                // RUN TESTS FOR THIS BRANCH
                if(!isIntegrationTest) {
                    String testResult;
                    try {
                        testResult = TestRunner.runTests(push.ref);
                        response.getWriter().println(testResult);
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().println("Error running tests: " + e.getMessage());
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }
            catch (InvalidPayloadException e)
            {
                // Malformed or missing JSON fields
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Invalid payload: " + e.getMessage());
            }

            baseRequest.setHandled(true);
        }
        else // Placeholder for other endpoints
        {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            System.out.println(target);

            response.getWriter().println("CI job done (placeholder)");
        }
    }

    /**
     * Starts the CI-server in command line
     *
     * @param args - Command-line arguments
     *
     * @throws Exception If the server fails to start or join the thread.
     */
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer());
        server.start();
        server.join();
    }
}
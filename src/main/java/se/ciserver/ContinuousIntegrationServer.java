package se.ciserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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

    private HttpClient httpClient;
    private String accessToken;
    private String latestTestOutput = "No tests run yet.";

    /**
     * Constructs the ContinuousIntegrationServer and starts a HttpClient
     * @param accessToken A githubs access token with commit status permission for the repository
     * 
     * @throws Exception if httpClient fails to start
     */
    public ContinuousIntegrationServer(String accessToken)
        throws Exception
    {
        this.accessToken = accessToken;
        
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        httpClient = new HttpClient(sslContextFactory);
        httpClient.start();
    }

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

                // Set commit status to pending
                String githubCommitUrl = "https://api.github.com/repos/"+push.repository.owner.name+"/"+push.repository.name+"/statuses/"+push.after;
                setCommitStatus(githubCommitUrl, "pending", "Testing in progress...", "ci_server");
                
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
                    if (result.testSuccess) {
                        System.out.println("Tests SUCCEEDED");
                        setCommitStatus(githubCommitUrl, "success", "All tests succeeded", "ci_server");
                    }
                    else {
                        System.out.println("Tests FAILED");
                        setCommitStatus(githubCommitUrl, "failure", "Test failures", "ci_server");
                    }   
                }
                else
                {
                    System.out.println("\nCompilation FAILED");
                    setCommitStatus(githubCommitUrl, "failure", "Compilation failed", "ci_server");
                }

                response.getWriter().println(result.output + "\n\n" + result.testOutput);
                latestTestOutput = "<pre>" + result.output + "\n\n" + result.testOutput + "</pre>";

                response.setStatus(HttpServletResponse.SC_OK);
                
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

            if (latestTestOutput != null) {
                response.getWriter().println(latestTestOutput);
            } else {
                response.getWriter().println("CI job done (placeholder)");
            }
        }
    }

    /**
     * Send a POST request setting the status of a github commit
     * @param url           - The url of the commit
     * @param status        - The status to set for the commit, "success", "failure" or "pending"
     * @param description   - Description of the status
     * @param context       - The system setting the status
     */
    public void setCommitStatus(String url,
                                String status,
                                String description,
                                String context)
    {
        // only runs if an accessToken was provided
        if (accessToken.equals("")) return;
        
        try {
            org.eclipse.jetty.client.api.Request request = httpClient.POST(url)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer "+accessToken)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .content(new StringContentProvider("{\"state\":\""+status+"\",\"description\":\""+description+"\",\"context\":\""+context+"\"}"), "application/json");
            
            ContentResponse response = request.send();

            if (response.getContentAsString().equals("{\"message\":\"Not Found\",\"documentation_url\":\"https://docs.github.com/rest/commits/statuses#create-a-commit-status\",\"status\":\"404\"}")) {
                System.out.println("Set Commit Status failed, possibly wrong repository url");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            // Post request failed
            System.out.println("Set Commit Status failed, post request exception");
        }
        
    }

    /**
     * Starts the CI-server in command line
     *
     * @param args - Command-line arguments, 1: a github access token to the repository
     *
     * @throws Exception If the server fails to start or join the thread.
     */
    public static void main(String[] args) throws Exception
    {
        String accessToken = "";
        if (args.length>0) {
            accessToken = args[0];
        }

        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer(accessToken));
        server.start();
        server.join();
    }
}
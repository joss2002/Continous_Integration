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

import se.ciserver.github.Push;
import se.ciserver.github.PushParser;
import se.ciserver.github.InvalidPayloadException;

/**
 * Github commit statuses
 */
enum CommitStatus {
    failure,
    pending,
    success,
}

/**
 * A Jetty-based CI-server that can start locally and receive HTTP-requests.
 */
public class ContinuousIntegrationServer extends AbstractHandler
{
    private final PushParser parser = new PushParser();
    public static boolean isIntegrationTest = false;
    private HttpClient httpClient;
    private String accessToken;

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
            String json = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            try
            {
                Push push = parser.parse(json);

                System.out.println("\nReceived push on branch : " + push.ref +
                                   "\nAfter SHA               : " + push.after +
                                   "\nRepository URL          : " + push.repository.clone_url +
                                   "\nPusher name             : " + push.pusher.name +
                                   "\n\nHead commit message     : " + push.head_commit.message);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Push received: " + push.after);

                setCommitStatus(push, CommitStatus.pending, "Testing in progres...", "ci_server");

                // RUN TESTS FOR THIS BRANCH
                if(!isIntegrationTest) {
                    String testResult;
                    try {
                        testResult = TestRunner.runTests(push.ref);
                        response.getWriter().println(testResult);
                        if (TestRunner.testSuccess)
                            setCommitStatus(push, CommitStatus.success, "All tests succeeded", "ci_server");
                        else
                            setCommitStatus(push, CommitStatus.failure, "Test failures", "ci_server");
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().println("Error running tests: " + e.getMessage());
                        setCommitStatus(push, CommitStatus.failure, "Error running tests: " + e.getMessage(), "ci_server");
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                }
                
            }
            catch (InvalidPayloadException e)
            {
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
     * Send a POST request setting the status of a git commit
     * @param push          - The git push to set status of
     * @param status        - The status to set for the commit
     * @param description   - Description of the status
     * @param context       - The system setting the status
     */
    public void setCommitStatus(Push push,
                                CommitStatus status,
                                String description,
                                String context)
    {
        String statusString = "";

        switch (status) {
            case failure:
                statusString = "failure";
                break;
            case pending:
                statusString = "pending";
                break;
            case success:
                statusString = "success";
                break;
        }
        
        try {
            // if push.repository.owner.name can be the full name filled into github this would fail, if its always the username then this works
            ContentResponse response = httpClient.POST("https://api.github.com/repos/"+push.repository.owner.name+"/"+push.repository.name+"/statuses/"+push.after)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer "+accessToken)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .content(new StringContentProvider("{\"state\":\""+statusString+"\",\"description\":\""+description+"\",\"context\":\""+context+"\"}"), "application/json")
                .send();

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
        if (args.length<1) {
            System.out.println("Too few arguments, needs 1: github access token");
            return;
        }

        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer(args[0]));
        server.start();
        server.join();
    }
}
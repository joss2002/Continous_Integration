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
import java.util.stream.Collectors;

import se.ciserver.github.Push;
import se.ciserver.github.PushParser;
import se.ciserver.github.Repository;
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

    public void setCommitStatus(Repository repository,
                                String commitSHA,
                                String authToken,
                                CommitStatus status,
                                String description,
                                String context
    ) {

        try {

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

            SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
            HttpClient client = new HttpClient(sslContextFactory);
            client.start();

            ContentResponse response = client.POST("https://api.github.com/repos/"+repository.owner+"/"+repository.name+"/statuses/"+commitSHA)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer "+authToken)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .content(new StringContentProvider("{\"state\":\""+statusString+"\",\"description\":\""+description+"\",\"context\":\""+context+"\"}"), "application/json")
                .send();

            client.stop();
        }
        catch (Exception e) {
            System.out.println(e);
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
package se.ciserver;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import se.ciserver.build.CompilationResult;
import se.ciserver.build.Compiler;
import se.ciserver.buildlist.Build;
import se.ciserver.buildlist.BuildStore;
import se.ciserver.github.InvalidPayloadException;
import se.ciserver.github.Push;
import se.ciserver.github.PushParser;

/**
 * A Jetty-based CI-server that can start locally and receive HTTP-requests.
 */
public class ContinuousIntegrationServer extends AbstractHandler
{

    private final PushParser parser   = new PushParser();
    private final Compiler   compiler = new Compiler();
    private final BuildStore store = new BuildStore("build-history.json");

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
                
                // Construct the current build into the history list
                Build build = Build.newBuild(push.after, push.ref, result.success, result.output);
                store.add(build);
                
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
        else if ("/builds".equals(target) && "GET".equalsIgnoreCase(request.getMethod())) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);

            StringBuilder html = new StringBuilder();
            html.append("<html><body><h1>Build history</h1><ul>");

            for (Build b : store.getAll()) {
                html.append("<li>")
                    .append("<a href=\"/builds/").append(b.id).append("\">")
                    .append(b.id).append("</a>")
                    .append(" — commit ").append(b.commitId)
                    .append(" (").append(b.branch).append(") ")
                    .append(b.timestamp)
                    .append(" [").append(b.status).append("]")
                    .append("</li>");
            }

            html.append("</ul></body></html>");
            response.getWriter().println(html.toString());
            baseRequest.setHandled(true);
        }
        else if (target.startsWith("/builds/") && "GET".equalsIgnoreCase(request.getMethod())) {
            String id = target.substring("/builds/".length());
            Build b = store.getById(id);

            if (b == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().println("Build not found");
            } else {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);

                StringBuilder html = new StringBuilder();
                html.append("<html><body>")
                    .append("<h1>Build ").append(b.id).append("</h1>")
                    .append("<p>Commit: ").append(b.commitId).append("</p>")
                    .append("<p>Branch: ").append(b.branch).append("</p>")
                    .append("<p>Date: ").append(b.timestamp).append("</p>")
                    .append("<p>Status: ").append(b.status).append("</p>")
                    .append("<h2>Log</h2>")
                    .append("<pre>").append(b.log).append("</pre>")
                    .append("</body></html>");

                response.getWriter().println(html.toString());
            }
            baseRequest.setHandled(true);
        }
        else if (target.startsWith("/builds") && "GET".equalsIgnoreCase(request.getMethod())) {
            // Build history endpoints
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);

            // Check if this is /builds or /builds/<id>
            if ("/builds".equals(target) || "/builds/".equals(target)) {
                // List view
                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>")
                    .append("<html><head><meta charset=\"utf-8\">")
                    .append("<title>Build history</title>")
                    .append("</head><body>")
                    .append("<h1>Build history</h1>");

                java.util.List<Build> builds = store.getAll();
                if (builds.isEmpty()) {
                    html.append("<p>No builds recorded yet.</p>");
                } else {
                    html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"4\">")
                        .append("<tr>")
                        .append("<th>Build ID</th>")
                        .append("<th>Commit</th>")
                        .append("<th>Branch</th>")
                        .append("<th>Timestamp</th>")
                        .append("<th>Status</th>")
                        .append("</tr>");

                    for (Build b : builds) {
                        html.append("<tr>")
                            .append("<td><a href=\"/builds/").append(b.id).append("\">")
                            .append(b.id)
                            .append("</a></td>")
                            .append("<td>").append(b.commitId).append("</td>")
                            .append("<td>").append(b.branch).append("</td>")
                            .append("<td>").append(b.timestamp).append("</td>")
                            .append("<td>").append(Boolean.TRUE.equals(b.status) ? "SUCCESS" : "FAILURE").append("</td>")
                            .append("</tr>");
                    }
                    html.append("</table>");
                }

                html.append("</body></html>");
                response.getWriter().println(html.toString());
                baseRequest.setHandled(true);
            } else {
                // Detail view: /builds/<id>
                String[] parts = target.split("/");
                String id = parts.length >= 3 ? parts[2] : null;

                if (id == null || id.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Missing build id");
                    baseRequest.setHandled(true);
                    return;
                }

                Build b = store.getById(id);
                if (b == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().println("Build not found: " + id);
                    baseRequest.setHandled(true);
                    return;
                }

                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>")
                    .append("<html><head><meta charset=\"utf-8\">")
                    .append("<title>Build ").append(b.id).append("</title>")
                    .append("</head><body>")
                    .append("<h1>Build ").append(b.id).append("</h1>")
                    .append("<p><strong>Commit:</strong> ").append(b.commitId).append("</p>")
                    .append("<p><strong>Branch:</strong> ").append(b.branch).append("</p>")
                    .append("<p><strong>Timestamp:</strong> ").append(b.timestamp).append("</p>")
                    .append("<p><strong>Status:</strong> ")
                    .append(Boolean.TRUE.equals(b.status) ? "SUCCESS" : "FAILURE")
                    .append("</p>")
                    .append("<h2>Build log</h2>")
                    .append("<pre>")
                    .append(b.log == null ? "" : b.log)
                    .append("</pre>")
                    .append("<p><a href=\"/builds\">Back to build history</a></p>")
                    .append("</body></html>");

                response.getWriter().println(html.toString());
                baseRequest.setHandled(true);
            }
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
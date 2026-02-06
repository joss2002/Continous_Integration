package se.ciserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;


/**
 * A Jetty-based CI-server that can start locally and recieve HTTP-requests.
 */
public class ContinuousIntegrationServer extends AbstractHandler
{
    /**
     * Handles incoming HTTP requests for the CI server.
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
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        System.out.println(target);

        response.getWriter().println("CI job done (placeholder)");
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
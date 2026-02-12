import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import se.ciserver.ContinuousIntegrationServer;
import se.ciserver.TestRunner;
import se.ciserver.TestUtils;
import se.ciserver.buildlist.Build;
import se.ciserver.buildlist.BuildStore;
import se.ciserver.github.InvalidPayloadException;
import se.ciserver.github.Push;
import se.ciserver.github.PushParser;
import se.ciserver.build.CompilationResult;
import se.ciserver.build.Compiler;

/**
 * Test class
 */
public class MainTest
{
    /**
     * Tests the CI-server for valid push event payload locally.
     *
     * @throws Exception If an input/output error occurs, if the server
     *                   fails to start or if sending the HTTP request
     *                   fails
     */
    @Test
    public void ciServerHandlePushValidPayloadLocal() throws Exception
    {

        Server server = new Server(0);
        server.setHandler(new ContinuousIntegrationServer(""));
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

        String json = TestUtils.readFile("githubPush.json");

        URL url = new URL("http://localhost:" + port + "/webhook");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream())
        {
            os.write(json.getBytes());
        }

        assertEquals(200, conn.getResponseCode());

        server.stop();
        server.join();
    }

    /**
     * Tests the CI-server for invalid push event payload locally.
     *
     * @throws Exception If an input/output error occurs, if the server
     *                   fails to start or if sending the HTTP request
     *                   fails
     */
    @Test
    public void ciServerHandlePushInvalidPayloadLocal() throws Exception
    {

        Server server = new Server(0);
        server.setHandler(new ContinuousIntegrationServer(""));
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

        String json = "{Invalid JSON}";

        URL url = new URL("http://localhost:" + port + "/webhook");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream())
        {
            os.write(json.getBytes());
        }

        assertEquals(400, conn.getResponseCode());

        server.stop();
        server.join();
    }

    /**
     * Tests the PushParser class with a valid GitHub push payload
     * JSON file.
     *
     * @throws Exception If the JSON file can not be read or if
     *                   parsing the payload fails
     */
    @Test
    public void pushParserValidPayload() throws Exception
    {
        String json = TestUtils.readFile("githubPush.json");
        PushParser parser = new PushParser();

        Push push = parser.parse(json);

        assertEquals("main", push.ref);
        assertEquals("e5f6g7h8", push.after);
        assertEquals("https://github.com/user/repo.git", push.repository.clone_url);
        assertEquals("name", push.pusher.name);
        assertEquals("Update README", push.head_commit.message);
    }

    /**
     * Tests the PushParser class with an invalid GitHub push payload
     * JSON file.
     *
     * @throws Exception If the payload can not be parsed
     */
    @Test(expected = InvalidPayloadException.class)
    public void pushParseInvalidPayload() throws Exception
    {
        String brokenJson = "{Invalid json}";

        PushParser parser = new PushParser();
        parser.parse(brokenJson);
    }

   /**
     * Tests that at least one test fails
     */
    @Test
    public void simpleTest() {
        int sum = 1+1;
        assertTrue(sum==2);
    }

    /**
     * Tests that setCommitStatus sends the correct request content
     * 
     * @throws Exception If an input/output error occurs, if the server
     *                   fails to start or if sending the HTTP request
     *                   fails
     */
    @Test
    public void testSetCommitStatusPostRequest() throws Exception
    {

        // Server to receive the POST request and check it
        class TestServer extends AbstractHandler {
            private boolean success = false;

            public void handle(String target,
                                Request baseRequest,
                                HttpServletRequest request,
                                HttpServletResponse response)
                throws IOException
            {

                if ("/webhook".equals(target) && "POST".equalsIgnoreCase(request.getMethod()))
                {
                    String requestString = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                    
                    if (requestString.equals("{\"state\":\"success\",\"description\":\"desc\",\"context\":\"context\"}"))
                        success = true;
                    baseRequest.setHandled(true);
                }
                else
                {
                    if (success) response.setStatus(HttpServletResponse.SC_OK);
                    else response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    baseRequest.setHandled(true);
                }

            }
        }

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer("github_acces_token");

        Server server = new Server(0);
        server.setHandler(new TestServer());
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

        ciServer.setCommitStatus("http://localhost:"+port+"/webhook", "success", "desc", "context");

        URL url = new URL("http://localhost:"+port+"/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        assertEquals(200, conn.getResponseCode());

        server.stop();
        server.join();        
    }

    /**
     * Tests that setCommitStatus outputs error message if url connection fails
     * 
     * @throws Exception If the server fails to start
     */
    @Test
    public void setCommitStatusFailsWithInvalidUrl()
        throws Exception
    {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream systemOutCatcher = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOutCatcher));

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer("github_acces_token");
        ciServer.setCommitStatus("http://invalid", "success", "desc", "context");

        assertEquals("Set Commit Status failed, post request exception\n", systemOutCatcher.toString());
        System.setOut(originalOut);
    }

    private static final String TEST_FILE = "build-history-test.json";
    
    /**
     * Test if build object can be created normally
     */
    @Test
    public void newBuild_setsAllFields() {
        String commitId = "abc123";
        String branch = "assessment";
        Boolean status = true;
        String log = "build output";

        Build b = Build.newBuild(commitId, branch, status, log);

        assertNotNull("id should be generated", b.id);
        assertFalse("id should not be empty", b.id.isEmpty());
        assertEquals("commit id should match", commitId, b.commitId);
        assertEquals("branch should match", branch, b.branch);
        assertEquals("status should match", status, b.status);
        assertEquals("log should match", log, b.log);
        assertNotNull("timestamp should be set", b.timestamp);
    }

    /**
     * Verify the behavior when history file does not exist yet
     */
    @Test
    public void newStoreWithoutFileStartsEmpty() {
        File f = new File(TEST_FILE);
        if (f.exists()) {
            assertTrue(f.delete());
        }

        BuildStore store = new BuildStore(TEST_FILE);

        List<Build> all = store.getAll();
        assertNotNull(all);
        assertTrue("store should start empty when file is missing", all.isEmpty());
    }

    /**
     * Checks whether the file persist and being reloaded after server restart,
     * simulated by having another BuildStore object
     */
    @Test
    public void addPersistsBuildAndCanBeReloaded() {
        // Cleanup
        File f = new File(TEST_FILE);
        if (f.exists()) {
            assertTrue(f.delete());
        }

        // first store: add one build
        BuildStore store1 = new BuildStore(TEST_FILE);
        Build build = Build.newBuild("commit1", "assessment",
                                     true, "log1");
        store1.add(build);

        List<Build> all1 = store1.getAll();
        assertEquals(1, all1.size());
        assertEquals(build.id, all1.get(0).id);

        // second store: simulate server restart by creating a new instance
        BuildStore store2 = new BuildStore(TEST_FILE);
        List<Build> all2 = store2.getAll();
        assertEquals("should load one build from file", 1, all2.size());
        Build loaded = all2.get(0);
        assertEquals(build.id, loaded.id);
    }

    /**
     * Test function returns the correct build with id search
     */
    @Test
    public void getByIdReturnsCorrectBuildOrNull() {
        BuildStore store = new BuildStore(TEST_FILE);
        Build b1 = Build.newBuild("c1", "assessment", true, "log1");
        Build b2 = Build.newBuild("c2", "assessment", false, "log2");

        store.add(b1);
        store.add(b2);

        Build found1 = store.getById(b1.id);
        assertNotNull(found1);
        assertEquals(b1.id, found1.id);

        Build found2 = store.getById(b2.id);
        assertNotNull(found2);
        assertEquals(b2.id, found2.id);

        Build missing = store.getById("does-not-exist");
        assertNull("unknown id should return null", missing);
    }

    /**
     * Tests that compiler returns failed compilation result for bad url, branch and SHA
     */
    @Test
    public void compilerReturnesFailedCompilationForBadInputs()
    {
        Compiler   compiler = new Compiler();
        CompilationResult result = compiler.compile(
                    "bad_url",
                    "bad_branch",
                    "bad_sha");
        
        assertFalse(result.success);
        assertFalse(result.testSuccess);

    }

    /**
     * Tests that CompilationResult correctly stores a successful build.
     */
    @Test
    public void compilationResultStoresSuccess()
    {
        CompilationResult result = new CompilationResult(true, true, "BUILD SUCCESS", "TEST SUCCESS");
        assertTrue(result.success);
        assertEquals("BUILD SUCCESS", result.output);
    }

    /**
     * Tests that CompilationResult correctly stores a failed build.
     */
    @Test
    public void compilationResultStoresFailure()
    {
        CompilationResult result = new CompilationResult(false, false, "BUILD FAILURE", "TEST FAILURE");
        assertFalse(result.success);
        assertEquals("BUILD FAILURE", result.output);
    }

    /**
     * Tests that the Compiler handles a clone failure gracefully
     * by returning a failed CompilationResult instead of throwing.
     */
    @Test
    public void compilerHandlesCloneFailure()
    {
        // Override createProcessBuilder to simulate a failing git clone
        Compiler failCompiler = new Compiler()
        {
            @Override
            protected ProcessBuilder createProcessBuilder(String... command)
            {
                return new ProcessBuilder("false");
            }
        };

        CompilationResult result = failCompiler.compile(
            "https://invalid-url.example.com/repo.git", "main", "abc123");

        assertFalse(result.success);
        assertNotNull(result.output);
    }

    /**
     * Tests that the Compiler returns a successful CompilationResult
     * when all process steps (clone, checkout, compile) succeed.
     */
    @Test
    public void compilerReturnsSuccessWhenAllStepsPass()
    {
        // Override createProcessBuilder to simulate all commands succeeding
        Compiler successCompiler = new Compiler()
        {
            @Override
            protected ProcessBuilder createProcessBuilder(String... command)
            {
                return new ProcessBuilder("true");
            }
        };

        CompilationResult result = successCompiler.compile(
            "https://example.com/repo.git", "main", "abc123");

        assertTrue(result.success);
    }
}
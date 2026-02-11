import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import se.ciserver.TestUtils;
import se.ciserver.github.PushParser;
import se.ciserver.github.Push;
import se.ciserver.github.InvalidPayloadException;


/**
 * Test class for parsing
 */
public class PushParserTest {
    
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
}
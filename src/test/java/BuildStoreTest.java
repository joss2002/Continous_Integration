import java.io.File;
import java.util.List;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import se.ciserver.buildlist.Build;
import se.ciserver.buildlist.BuildStore;

public class BuildStoreTest {

    private static final String TEST_FILE = "build-history-test.json";

    /**
     * Clean the old build list 
     */
    @After
    public void cleanup() {
        File f = new File(TEST_FILE);
        if (f.exists()) {
            assertTrue(f.delete());
        }
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

    @Test
    public void addPersistsBuildAndCanBeReloaded() {
        // first store: add one build
        BuildStore store1 = new BuildStore(TEST_FILE);
        Build build = Build.newBuild("commit1", "assessment",
                                     Build.Status.SUCCESS, "log1");
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
        Build b1 = Build.newBuild("c1", "assessment", Build.Status.SUCCESS, "log1");
        Build b2 = Build.newBuild("c2", "assessment", Build.Status.FAILURE, "log2");

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
}

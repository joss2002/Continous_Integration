package se.ciserver.buildlist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.ciserver.buildlist.Build;  

/**
 * Handles persistent storage of Build history.
 */
public class BuildStore {

    private final File storeFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Build> builds = new ArrayList<>();

    /**
     * Creates a BuildStore pointing at the given file path and loads existing history.
     *
     * @param filePath Location of the history file
     */
    public BuildStore(String filePath) {
        this.storeFile = new File(filePath);
        load();
    }

    /**
     * Returns a read-only copy of the build history.
     *
     * @return An unmodifiable list of all builds
     */
    public synchronized List<Build> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(builds));
    }

    /**
     * Returns the build with the given id, or null if not found.
     *
     * @param id The build identifier
     *
     * @return The matching Build, or null if no build has the given id
     */
    public synchronized Build getById(String id) {
        return builds.stream()
                .filter(b -> b.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a build to history and immediately persists it.
     *
     * @param build the Build to store
     */
    public synchronized void add(Build build) {
        builds.add(build);
        save();
    }

    /**
     * Loads build history from the backing file if it exists.
     * Any exception is printed to the server console.
     */
    private void load() {
        try {
            if (!storeFile.exists()) {
                return;
            }
            byte[] bytes = Files.readAllBytes(storeFile.toPath());
            if (bytes.length == 0) {
                return;
            }
            List<Build> loaded = mapper.readValue(
                    bytes,
                    new TypeReference<List<Build>>() {}
            );
            builds.clear();
            builds.addAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Persists the current build history to the backing file.
     */
    private void save() {
        try {
            byte[] bytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(builds);
            Files.write(storeFile.toPath(), bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

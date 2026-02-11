package se.ciserver.buildlist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the storage of build and related
 */
public class BuildStore {

    private final File storeFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Build> builds = new ArrayList<>();

    /**
     * Creates an File object in the program referring to the BuildList
     * 
     * @param filePath  Location of where the file is located
     */
    public BuildStore(String filePath) {
        this.storeFile = new File(filePath);
        load();
    }

    /**
     * Extract the read-only list of build history (build list)
     */
    public synchronized List<Build> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(builds));
    }

    /**
     * Extract a specific build history by its unique id
     * 
     * @param id    The identifier of the requested build history
     */
    public synchronized Build getById(String id) {
        return builds.stream()
                .filter(b -> b.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add another build into the build history and immediately save it into file
     * 
     * @param build The Build object that stores the information of current build
     */
    public synchronized void add(Build build) {
        builds.add(build);
        save();
    }

    /**
     * Load the file as described in storeFile
     * If the funciton results in an exception, the corresponding exception will be printed in the server console
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
     * Save the build into the file
     */
    private void save() {
        try {
            byte[] bytes = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(builds);
            Files.write(storeFile.toPath(), bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

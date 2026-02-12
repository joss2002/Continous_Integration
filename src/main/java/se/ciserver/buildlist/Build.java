package se.ciserver.buildlist;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines a build object that stores the information of each build by the CI-server
 */
public class Build {

    public String id;
    public String commitId;
    public String branch;
    public String timestamp;
    public Boolean status;
    public String log;

    /**
     * Default constructor for Build, required for JSON deserialization.
     */
    public Build(){
    }

    /**
     * Constructs a Build object with all fields.
     *
     * @param id        Unique identifier for the build
     * @param commitId  The git commit SHA that triggered this build
     * @param branch    The branch that the commit is from
     * @param timestamp When this build occurred (ISO 8601)
     * @param status    Whether this build was successful
     * @param log       Output produced during the build and tests
     */
    public Build(String id, String commitId, String branch,
                 String timestamp, Boolean status, String log) {
        this.id = id;
        this.commitId = commitId;
        this.branch = branch;
        this.timestamp = timestamp;
        this.status = status;
        this.log = log;
    }

    /**
     * Factory method that creates a new Build with a generated UUID and current timestamp.
     *
     * @param commitId The git commit SHA
     * @param branch   The branch name
     * @param status   Whether the build succeeded
     * @param log      The build output log
     *
     * @return A new Build instance
     */
    public static Build newBuild(String commitId, String branch,
                                 Boolean status, String log) {
        return new Build(
                UUID.randomUUID().toString(),
                commitId,
                branch,
                Instant.now().toString(),
                status,
                log
        );
    }

}

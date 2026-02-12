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
     * Default Consturctor of Build
     */
    public Build(){
    }

    /**
     * Constructs a Build objects
     * 
     * @param id        Identifier for the build
     * @param comitId   Identifier for the git commit SHA that triggers this build
     * @param branch    Branch that the commit is from
     * @param timestamp When this build is occurred
     * @param status    Whether this build was successful or not
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

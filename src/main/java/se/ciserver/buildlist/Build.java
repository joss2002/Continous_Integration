package se.ciserver.buildlist;

import java.time.Instant;

/**
 * Defines a build object that stores the information of each build by the CI-server
 */
public class Build {

    public enum Status { SUCCESS, FAILURE }

    public final String id;
    public final String commitId;
    public final String branch;
    public final Instant timestamp;
    public final Status status;
    public final String log;

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
                 Instant timestamp, Status status, String log) {
        this.id = id;
        this.commitId = commitId;
        this.branch = branch;
        this.timestamp = timestamp;
        this.status = status;
        this.log = log;
    }

}

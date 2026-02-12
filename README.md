## Host CI-server locally

### Prerequisites
- Java 8
- Java JDK 17
- Maven 3.9
- Git

<details>
<summary id="run-the-server"><span style="font-size:15px; font-weight:bold;">Run the server</span></summary>

1. Clone the repository

```bash
git clone https://github.com/joss2002/Continuous_Integration.git
```

2. Navigate to the repository and build the project with **Maven**

```bash
mvn clean compile
```

3. Run the CI-server

Without commit status notifications:
```bash
mvn exec:java
```

With commit status notifications:

To set the status of commits, a [github access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) with commit status permission is required.
```bash
mvn exec:java -Dexec.mainClass=ci_server.Main -Dexec.args="<GITHUB ACCESS TOKEN>"
```

</details>

<details>
<summary><span style="font-size:15px; font-weight:bold;">Test the server</span></summary>

**Option 1:** Use curl
```bash
curl http://localhost:8080/
```

<details>
<summary><span style="font-size:13px;">Expected output</span></summary>

```console
StatusCode        : 200
StatusDescription : OK
Content           : CI job done (placeholder)
```

</details>

**Option 2:** Open browser at `http://localhost:8080/`

<details>
<summary><span style="font-size:13px;">Expected output</span></summary>

```text
CI job done (placeholder)
```

</details>

> Note: 8080 is the default port number.

</details>

### Perform local push event successfully

1. Run the server, see [Run the server](#run-the-server).

<details>
<summary id="run-the-server"><span style="font-size:15px">Bash</span></summary>

2. Load local `JSON` payload test file

```bash
json=$(cat ./src/test/resources/githubPush.json)
```

3. Post JSON payload to the CI-server

```bash
curl http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d "$json"
```

<details>
<summary><span style="font-size:13px;">Expected output</span></summary>

Client side
```console
HTTP/1.1 200 OK
Content-Length: 25
Date: Tue, 10 Feb 2026 12:27:04 GMT
Server: Jetty(9.4.50.v20221201)

Push received: e5f6g7h8
```

Server side
```console
Received push on branch : main
After SHA               : e5f6g7h8
Repository URL          : https://github.com/user/repo.git
Pusher name             : name

Head commit message     : Update README
```

</details>

</details>

<details>
<summary id="run-the-server"><span style="font-size:15px">PowerShell</span></summary>

2. Load local `JSON` payload test file

```bash
$json = Get-Content -Raw "./src/test/resources/githubPush.json"
```

3. Post JSON payload to the CI-server

```bash
Invoke-WebRequest -Uri http://localhost:8080/webhook `
  -Method POST `
  -Body $json `
  -ContentType "application/json"
```

<details>
<summary><span style="font-size:13px;">Expected output</span></summary>

Client side
```console
StatusCode        : 200
StatusDescription : OK
Content           : {80, 117, 115, 104...}
RawContent        : HTTP/1.1 200 OK
                    Content-Length: 25
                    Date: Tue, 10 Feb 2026 12:27:04 GMT
                    Server: Jetty(9.4.50.v20221201)

                    Push received: e5f6g7h8
```

Server side
```console
Received push on branch : main
After SHA               : e5f6g7h8
Repository URL          : https://github.com/user/repo.git
Pusher name             : name

Head commit message     : Update README
```

</details>

</details>

### Test invalid payload

<details>
<summary id="run-the-server"><span style="font-size:15px">Bash</span></summary>

Post invalid `JSON` to the CI-server

```bash
curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d '{Invalid JSON}'
```

<details>
<summary><span style="font-size:13px;">Expected output</span></summary>

```console
Invalid payload: Invalid GitHub push payload
```

</details>

</details>

<details>
<summary id="run-the-server"><span style="font-size:15px">PowerShell</span></summary>

Post invalid `JSON` to the CI-server

```bash
Invoke-WebRequest `
  -Uri http://localhost:8080/webhook `
  -Method POST `
  -Body '{Invalid JSON}' `
  -ContentType "application/json"
```

<details>
<summary><span style="font-size:13px;">Expected output</span></summary>

```console
Invoke-WebRequest : Invalid payload: Invalid GitHub push payload
At line:1 char:1
+ Invoke-WebRequest -Uri http://localhost:8080/webhook -Method POST -Bo ...
+ ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : InvalidOperation: (System.Net.HttpWebRequest:HttpWebRequest) [Invoke-WebRequest], WebExc
   eption
    + FullyQualifiedErrorId : WebCmdletWebResponseException,Microsoft.PowerShell.Commands.InvokeWebRequestCommand
```
</details>

</details>

### Compilation (P1)

#### Implementation

When the CI server receives a GitHub push webhook on `/webhook`, it triggers compilation of the pushed branch. The `Compiler` class in `se.ciserver.build` performs the following steps:

1. Creates a temporary directory
2. Clones the specific branch using `git clone --branch <branch> --single-branch <url>`
3. Checks out the exact commit SHA with `git checkout <sha>`
4. Runs `mvn clean compile` in the cloned project
5. Captures and prints the build output to the server console
6. Cleans up the temporary directory

The compilation result (success/failure) is returned in the HTTP response and printed to the server console.

#### Unit testing

Compilation is unit-tested in `src/test/java/MainTest.java` with the following tests:

- `compilationResultStoresSuccess()` — verifies that a successful `CompilationResult` stores `success=true` and the build output.
- `compilationResultStoresFailure()` — verifies that a failed `CompilationResult` stores `success=false` and the build output.
- `compilerHandlesCloneFailure()` — subclasses `Compiler` to override `createProcessBuilder()` with a failing command, verifying that a clone failure returns `success=false` without throwing an exception.
- `compilerReturnsSuccessWhenAllStepsPass()` — subclasses `Compiler` to override `createProcessBuilder()` with a succeeding command, verifying the full pipeline returns `success=true`.
- `ciServerHandleCompilationOnPush()` — starts a local Jetty server, sends a valid push payload to `/webhook`, and verifies the response is `200` and contains the compilation result with the commit SHA.

To run the tests, see [Perform unit tests](#perform-unit-tests).

---

## Perform unit tests

1. Build the project with **Maven**

```bash
mvn clean compile
```

2. Run the `junit` unit tests

```bash
mvn test
```

3. Verify all tests pass in the output

```console
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

### Connect the server to a webhook with ngrok

1. Run the server, see [Run the server](#run-the-server).

2. In a separate terminal, start ngrok to expose port `8080`:

```bash
ngrok http 8080
```

3. Copy the forwarding URL (e.g. `https://xxxx.ngrok-free.app`) from the ngrok output.

4. In your GitHub repository, go to **Settings > Webhooks > Add webhook** and set:
   - **Payload URL**: `https://xxxx.ngrok-free.app/webhook`
   - **Content type**: `application/json`
   - **Events**: Select "Just the push event"

5. Push a commit to the repository and observe the compilation output in the server console.
---

## Automated Test Execution via Github Push Events

When a push is made to the git repository, Github sends a HTTP POST request (Webhook) to the running server. From the request and its payload, the branch to which the push was made can be extracted. Upon parsing the payload, the server checks out to the target branch, pulls the latest changes and runs the project's test suite.

The automated test logic is handled by the `TestRunner` class which is responsible for executing the following:

<details>
<summary id="test-functionality"><span style="font-size:15px; font-weight:bold;">Functionality</span></summary>

1. Checkout the pushed branch using Git.

2. Running the test suite using `mvn test`

3. Capturing the output and exit status

4. Returning the test logs, displayed both in server terminal and in the HTTP response.

</details>


<details>
<summary id="test-functionality"><span style="font-size:15px; font-weight:bold;">Test the functionality</span></summary>

1. Run the server, see [Run the server](#run-the-server).

2. Configure Webhook from server to repository.

2. Expose server to Github using `ngrok`.

3. Observe response upon a Github push event in terminal or HTTP response.

</details>

## Notification Implementation and Testing

Notifications are implemented by setting the status of commits using github's REST api. A post request containing the status is sent to the url of the push's last commit.

The notification implementation is tested by running a test server and sending the status post request to it instead, which checks that its contents are correct.

### Unit testing of test execution logic
To avoid using real Git and Maven commands during unit testing, the `TestRunner` class uses a command hook mechanism that intercepts command execution. When this hook mechanism is set, command are captured rather than executed and the expected behaviour could be asserted within the unit test. 

## Build History

When the CI server receives a GitHub push webhook on `/webhook`, it compiles the pushed commit and records a build entry in a persistent build history. The history is stored as JSON on disk and loaded again when the server restarts, so past builds are preserved across reboots. The build history is exposed via HTTP so it can be browsed in a browser.

### Build list URL

The list of all builds is available at:

```text
http://<server-host>:8080/builds
````

### Individual Build URL

Each build has a unique URL of the form:

```text
http://<server-host>:8080/builds/<build-id>
````

## Statements of contributions

| Name                       | Contribution                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Josefine "joss2002" Nyholm | <ul><li>[x] Added initial **Maven** file-tree structure, including a basic `pom.xml` with **JDK 17** support.</li></ul><ul><li>[x] Added skeleton for CI-server in `src/main/java/se/ciserver/ContinuousIntegrationServer.java`.<ul><li>Added related dependencies, plugins; `jetty-server`, `exec-maven-plugin`</li><li>[x] Added additional documentation.</li></ul></li></ul><ul><li>[x] Added **GitHub** push event `JSON` payload component.<ul><li>[x] Added required Webhook payload object parameters and classes for `push` within `src/main/java/se/ciserver/github` including files; `PushParser.java`, `Push.java`, `Pusher.java`, `Commit.java`, `Author.java` and `Repository.java`.</li><li>[x] Added additional `Exception` extension in `InvalidPayloadException.java` specified for invalid payloads.</li><li>[x] Integrated the push parsing `JSON` payload functionality in the `ContinuousIntegrationServer.java` `handler()` to allow the CI-server to receive `JSON` payloads and present the relevant variables; `ref`, `after`, `repository.clone_url`, `pusher.name`, `head_commit.message`.</li><li>[x] Added unit tests in `src/test/java/MainTest.java` for `PushParser.java` push parsing `JSON` payload functionality and local testing of the `ContinuousIntegrationServer.java` CI-server handling of push parsing `JSON` payloads. This, as well as an additional file `src/main/java/se/ciserver/TestUtils.java` including test utilities such as reading filed. Supporting the unit tests a test `JSON` file `src/main/test/resources/githubPush.java` was added to represent a typical push event payload.</li><li>[x] Added additional dependencies in `pom.xml` for `jackson-databind` and `junit`</li><li>[x] Added additional documentation in `README.md`.</li></ul></li></ul> |
| Avid "HotFazz" Fayaz | <ul><li>[x] Added webhook-triggered compilation (P1) in `src/main/java/se/ciserver/build/Compiler.java`.<ul><li>[x] Clones the pushed branch, checks out the exact commit, and runs `mvn clean compile`.</li><li>[x] Added `CompilationResult.java` to hold build outcome and output.</li><li>[x] Integrated compilation into `ContinuousIntegrationServer.java` webhook handler.</li><li>[x] Added unit tests in `MainTest.java` for `CompilationResult` and `Compiler` failure handling.</li><li>[x] Added documentation in `README.md` for compilation implementation and unit testing.</li></ul></li></ul> |
| Albin "zzimbaa" Blomqvist | <ul><li>[x] Implemented automated test execution triggered by GitHub push webhooks.</li></ul><ul><li>[x] Added `TestRunner.java` in `src/main/java/se/ciserver/` to handle CI test execution.<ul><li>[x] Dynamically checks out the pushed branch using git checkout and updates it with git pull.</li><li>[x] Executes Maven test suite using `mvn test`.</li><li>[x] Captures test output via `ProcessBuilder` and write logs to both `terminal` and `HTTP response` indicating test success or failure.</li><li>[x] Determines build success/failure based on process exit code.</li><li>[x] Added unit test for `runTests` method to verify correct correct branch checkout and pull commands using command hook.</li></ul></li></ul><ul><li>[x] Integrated the test execution logic into `ContinuousIntegrationServer.java` so that tests are triggered automatically upon receiving a GitHub push webhook event.</li></ul><ul><li>[x] Verified webhook-based test execution using `ngrok` for local tunneling and GitHub webhook deliveries.</li></ul><ul><li>[x] Added documentation in `README.md` describing how to trigger automated tests via GitHub push events.</li></ul> |
| Erik Olsson "erik-ol" | <ul><li>[x] Added **GitHub** commit status setter component.<ul><li>[x] Added `ContinuousIntegrationServer()` `startHttpClient()` and `setCommitStatus()` to `ContinuousIntegrationServer.java` <li>[x] Extended `ContinuousIntegrationServer.java` `handler()` to set commit status of recieved pushes. <li>[x] Added unit tests in `src/test/java/MainTest.java` for `setCommitStatus()` sending post request functionality and failing due to invalid url. <li>[x] Implemented github access token handling <li>[x] Added additional dependencies in `pom.xml` for `jetty-client` <li>[x] Added documentation for commit status implementation and testing in `README.md`. </li></ul></li></ul> |
| Albin "zzimbaa" Blomqvist | <ul><li>[x] Implemented automated test execution triggered by GitHub push webhooks.</li></ul><ul><li>[x] Added `TestRunner.java` in `src/main/java/se/ciserver/` to handle CI test execution.<ul><li>[x] Dynamically checks out the pushed branch using git checkout and updates it with git pull.</li><li>[x] Executes Maven test suite using `mvn test`.</li><li>[x] Captures test output via `ProcessBuilder` and write logs to both `terminal` and `HTTP response` indicating test success or failure.</li><li>[x] Determines build success/failure based on process exit code.</li><li>[x] Added unit test for `runTests` method to verify correct correct branch checkout and pull commands using command hook.</li></ul></li></ul><ul><li>[x] Integrated the test execution logic into `ContinuousIntegrationServer.java` so that tests are triggered automatically upon receiving a GitHub push webhook event.</li></ul><ul><li>[x] Verified webhook-based test execution using `ngrok` for local tunneling and GitHub webhook deliveries.</li></ul><ul><li>[x] Added documentation in `README.md` describing how to trigger automated tests through GitHub push events.</li></ul> |
| Pun Chun "MrNoodlez-1227" Chow | <ul><li>[x] Implemented persistent build history with unique URLs. <ul><li>[x] Added the `Build` model in `src/main/java/se/ciserver/buildlist/Build.java` to represent individual CI builds (commit id, branch, timestamp, status, and logs).</li><li>[x] Implemented `BuildStore` in `src/main/java/se/ciserver/buildlist/BuildStore.java` to persist build history as JSON on disk, including loading on startup, adding new builds, and looking up builds by id.</li><li> [x] Extended `ContinuousIntegrationServer` to wire in `BuildStore`, and to expose the `/builds` endpoint for listing all builds and the `/builds/<id>` endpoint for viewing a single build’s information and log output.</li><li>[x] Added JUnit tests under `src/test/java/MainTest.java` to verify correct construction of Build objects and that BuildStore correctly saves and reloads history from file across server restarts.</li><li>[x] Updated this README with documentation for the build list URL and unique build URLs so graders can browse the history directly. 
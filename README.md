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

## Perform unit tests

1. Build the project with **Maven**

```bash
mvn clean compile
```

2. Run the `junit` unit tests

```bash
mvn test
```
---

## Automated Test Execution via Github Push Events

When a push is made to the git repository, Github sends a HTTP POST request (Webhook) to the running server. From the request and its payload, the branch to which the push was made can be extracted. Upon parsing the payload, the server checks out to the target branch, pulls the latest changes and runs the project's test suite.

<details>
<summary id="test-functionality"><span style="font-size:15px; font-weight:bold;">Test functionality</span></summary>

1. Run the server, see [Run the server](#run-the-server).


2. Expose server to Github using `ngrok`

Configure Github Webhook with the ngrok forwarding URL (e.g https://.../webhook)

3. See response

When a push is made, the CI server automatically executes the tests on the pushed branch. The result can be viewed in terminal or in the ngrok web interface.

</details>

## Notification Implementation and Testing

Notifications are implemented by setting the status of commits using github's REST api. A post request containing the status is sent to the url of the push's last commit.

The notification implementation is tested by running a test server and sending the status post request to it instead, which checks that its contents are correct.

## Statements of contributions

| Name                       | Contribution                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Josefine "joss2002" Nyholm | <ul><li>[x] Added initial **Maven** file-tree structure, including a basic `pom.xml` with **JDK 17** support.</li></ul><ul><li>[x] Added skeleton for CI-server in `src/main/java/se/ciserver/ContinuousIntegrationServer.java`.<ul><li>Added related dependencies, plugins; `jetty-server`, `exec-maven-plugin`</li><li>[x] Added additional documentation.</li></ul></li></ul><ul><li>[x] Added **GitHub** push event `JSON` payload component.<ul><li>[x] Added required Webhook payload object parameters and classes for `push` within `src/main/java/se/ciserver/github` including files; `PushParser.java`, `Push.java`, `Pusher.java`, `Commit.java`, `Author.java` and `Repository.java`.</li><li>[x] Added additional `Exception` extension in `InvalidPayloadException.java` specified for invalid payloads.</li><li>[x] Integrated the push parsing `JSON` payload functionality in the `ContinuousIntegrationServer.java` `handler()` to allow the CI-server to receive `JSON` payloads and present the relevant variables; `ref`, `after`, `repository.clone_url`, `pusher.name`, `head_commit.message`.</li><li>[x] Added unit tests in `src/test/java/MainTest.java` for `PushParser.java` push parsing `JSON` payload functionality and local testing of the `ContinuousIntegrationServer.java` CI-server handling of push parsing `JSON` payloads. This, as well as an additional file `src/main/java/se/ciserver/TestUtils.java` including test utilities such as reading filed. Supporting the unit tests a test `JSON` file `src/main/test/resources/githubPush.java` was added to represent a typical push event payload.</li><li>[x] Added additional dependencies in `pom.xml` for `jackson-databind` and `junit`</li><li>[x] Added additional documentation in `README.md`.</li></ul></li></ul> |
| Erik Olsson "erik-ol" | <ul><li>[x] Added **GitHub** commit status setter component.<ul><li>[x] Added `ContinuousIntegrationServer()` `startHttpClient()` and `setCommitStatus()` to `ContinuousIntegrationServer.java` <li>[x] Extended `ContinuousIntegrationServer.java` `handler()` to set commit status of recieved pushes. <li>[x] Added unit tests in `src/test/java/MainTest.java` for `setCommitStatus()` sending post request functionality and failing due to invalid url. <li>[x] Implemented github access token handling <li>[x] Added additional dependencies in `pom.xml` for `jetty-client` <li>[x] Added documentation for commit status implementation and testing in `README.md`. </li></ul></li></ul> |
| Albin "zzimbaa" Blomqvist | <ul><li>[x] Implemented automated test execution triggered by GitHub push webhooks.</li></ul><ul><li>[x] Added `TestRunner.java` in `src/main/java/se/ciserver/` to handle CI test execution.<ul><li>[x] Dynamically checks out the pushed branch using git checkout and updates it with git pull.</li><li>[x] Executes Maven test suite using `mvn test`.</li><li>[x] Captures test output via `ProcessBuilder` and write logs to both `terminal` and `HTTP response` indicating test success or failure.</li><li>[x] Determines build success/failure based on process exit code.</li><li>[x] Added unit test for `runTests` method to verify correct correct branch checkout and pull commands using command hook.</li></ul></li></ul><ul><li>[x] Integrated the test execution logic into `ContinuousIntegrationServer.java` so that tests are triggered automatically upon receiving a GitHub push webhook event.</li></ul><ul><li>[x] Verified webhook-based test execution using `ngrok` for local tunneling and GitHub webhook deliveries.</li></ul><ul><li>[x] Added documentation in `README.md` describing how to trigger automated tests through GitHub push events.</li></ul> |

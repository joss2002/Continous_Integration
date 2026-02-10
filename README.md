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

```bash
mvn exec:java
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

Compilation is unit-tested in `src/test/java/MainTest.java`:

- `compilationResultStoresValues()` — verifies that `CompilationResult` correctly stores the success flag and build output.
- `compilerHandlesCloneFailure()` — subclasses `Compiler` to override `createProcessBuilder()` with a failing command, verifying that a clone failure returns `success=false` without throwing an exception.

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
---

## Statements of contributions

| Name                       | Contribution                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Josefine "joss2002" Nyholm | <ul><li>[x] Added initial **Maven** file-tree structure, including a basic `pom.xml` with **JDK 17** support.</li></ul><ul><li>[x] Added skeleton for CI-server in `src/main/java/se/ciserver/ContinuousIntegrationServer.java`.<ul><li>Added related dependencies, plugins; `jetty-server`, `exec-maven-plugin`</li><li>[x] Added additional documentation.</li></ul></li></ul><ul><li>[x] Added **GitHub** push event `JSON` payload component.<ul><li>[x] Added required Webhook payload object parameters and classes for `push` within `src/main/java/se/ciserver/github` including files; `PushParser.java`, `Push.java`, `Pusher.java`, `Commit.java`, `Author.java` and `Repository.java`.</li><li>[x] Added additional `Exception` extension in `InvalidPayloadException.java` specified for invalid payloads.</li><li>[x] Integrated the push parsing `JSON` payload functionality in the `ContinuousIntegrationServer.java` `handler()` to allow the CI-server to receive `JSON` payloads and present the relevant variables; `ref`, `after`, `repository.clone_url`, `pusher.name`, `head_commit.message`.</li><li>[x] Added unit tests in `src/test/java/MainTest.java` for `PushParser.java` push parsing `JSON` payload functionality and local testing of the `ContinuousIntegrationServer.java` CI-server handling of push parsing `JSON` payloads. This, as well as an additional file `src/main/java/se/ciserver/TestUtils.java` including test utilities such as reading filed. Supporting the unit tests a test `JSON` file `src/main/test/resources/githubPush.java` was added to represent a typical push event payload.</li><li>[x] Added additional dependencies in `pom.xml` for `jackson-databind` and `junit`</li><li>[x] Added additional documentation in `README.md`.</li></ul></li></ul> |
| Avid "HotFazz" Fayaz | <ul><li>[x] Added webhook-triggered compilation (P1) in `src/main/java/se/ciserver/build/Compiler.java`.<ul><li>[x] Clones the pushed branch, checks out the exact commit, and runs `mvn clean compile`.</li><li>[x] Added `CompilationResult.java` to hold build outcome and output.</li><li>[x] Integrated compilation into `ContinuousIntegrationServer.java` webhook handler.</li><li>[x] Added unit tests in `MainTest.java` for `CompilationResult` and `Compiler` failure handling.</li><li>[x] Added documentation in `README.md` for compilation implementation and unit testing.</li></ul></li></ul> |

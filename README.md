## Host CI-server locally

### Prerequisites
- Java 8
- Java JDK 17
- Maven 3.9
- Git

<details>
<summary><span style="font-size:15px; font-weight:bold;">Run the server</span></summary>

1. Clone the repository

```bash
git clone https://github.com/joss2002/Continuous_Integration.git
```

2. Build the project with Maven

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

---

## Statements of contributions

| Name                           | Contribution                                                                                                                                                                                                                                                                                 |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Josefine "joss2002" Nyholm     | <ul><li>[x] Added initial **Maven** file-tree structure, including a basic `pom.xml` with **JDK 17** support.</li></ul><ul><li>[x] Added skeleton for CI-server in `src/main/java/se/ContinuousIntegrationServer.java` as well as related dependencies, plugins and documentation.</li></ul> |

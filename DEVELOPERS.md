# Build

OmniFaces 3.x requires Java 8 to build.

```bash
mvn clean install -Dmaven.test.skip=true
```

Without `-Dmaven.test.skip=true`, the build also runs the integration tests on the default server profile, as described below.

# Run integration tests

The integration tests run with Arquillian against a real server, selected by a Maven profile.
The `wildfly` profile is active by default:

```bash
mvn clean verify
```

To run on a different server, select its profile:

```bash
mvn clean verify -P tomcat-mojarra
```

Available profiles are `wildfly`, `tomee`, `payara`, `tomcat-mojarra` and `tomcat-myfaces`.
CI runs all of them on Java 8.

The tests use the HtmlUnit browser, so no browser needs to be installed.

To run a single test class:

```bash
mvn clean verify -Dit.test=EagerIT
```

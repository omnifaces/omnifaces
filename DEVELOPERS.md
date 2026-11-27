# Build

OmniFaces 3.x requires Java 8 to build.

```bash
mvn clean install
```

This runs the unit tests, but not the integration tests.

To skip all tests:

```bash
mvn clean install -Dmaven.test.skip=true
```

# Run integration tests

The integration tests run with Arquillian against a real server, selected by a Maven profile.
No profile is active by default, so a profile is required:

```bash
mvn clean verify -P wildfly
```

Available profiles are `wildfly`, `tomee`, `payara`, `tomcat-mojarra` and `tomcat-myfaces`.
CI runs all of them on Java 8.

The tests use the HtmlUnit browser, so no browser needs to be installed.

To run a single test class:

```bash
mvn clean verify -P wildfly -Dit.test=EagerIT
```

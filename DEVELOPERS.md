# Build

OmniFaces 4.x requires Java 11 to build.

```bash
mvn clean install
```

This runs the unit tests, but not the integration tests.
The TypeScript sources are built by `frontend-maven-plugin`, which downloads its own Node.js, so no local Node.js installation is needed.

To skip all tests:

```bash
mvn clean install -Dmaven.test.skip=true
```

# Run integration tests

The integration tests run with Arquillian against a real server, selected by a Maven profile.
No profile is active by default, so a profile is required:

```bash
mvn clean verify -P wildfly-mojarra4
```

Available profiles are `wildfly-mojarra4`, `liberty-myfaces3`, `glassfish-mojarra4`, `tomee-myfaces4`, `tomcat-mojarra4`, `tomcat-mojarra3`, `tomcat-myfaces4`, `tomcat-myfaces3` and `payara-mojarra4`.
WildFly needs Java 17.
CI therefore runs all profiles on Java 17.

The tests drive Chrome through Selenium, so Chrome must be installed.
To use a different browser:

```bash
mvn clean verify -P wildfly-mojarra4 -Darquillian.browser=firefox
```

To run a single test class:

```bash
mvn clean verify -P wildfly-mojarra4 -Dit.test=EagerIT
```

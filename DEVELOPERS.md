# Build

OmniFaces 5.x requires Java 17 to build.

```bash
mvn clean install
```

This runs the unit tests and the TypeScript tests, but not the integration tests.
The TypeScript sources are built by `frontend-maven-plugin`, which downloads its own Node.js, so no local Node.js installation is needed.

To skip all tests and the Javadoc:

```bash
mvn clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```

# Run integration tests

The integration tests run with Arquillian against a real server, selected by a Maven profile.
No profile is active by default, so a profile is required:

```bash
mvn clean verify -P wildfly-mojarra
```

Available profiles are `wildfly-mojarra`, `liberty-myfaces`, `glassfish-mojarra`, `glassfish-mojarra5`, `tomee-myfaces`, `tomcat-mojarra`, `tomcat-mojarra5`, `tomcat-myfaces`, `tomcat-myfaces5`, `payara-mojarra` and `quarkus-myfaces`.
GlassFish and Payara need Java 21.
CI therefore runs all profiles on Java 21.

The tests drive Chrome through Selenium, so Chrome must be installed.
To use a different browser:

```bash
mvn clean verify -P wildfly-mojarra -Darquillian.browser=firefox
```

To run a single test class:

```bash
mvn clean verify -P wildfly-mojarra -Dit.test=EagerIT
```

To rerun failing tests, which helps against flaky browser timing:

```bash
mvn clean verify -P wildfly-mojarra -Dfailsafe.rerunFailingTestsCount=2
```

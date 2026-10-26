# Build

OmniFaces 4.x requires Java 11 to build.

```bash
mvn clean install -Dmaven.test.skip=true
```

The TypeScript sources are built by `frontend-maven-plugin`, which downloads its own Node.js, so no local Node.js installation is needed.
Without `-Dmaven.test.skip=true`, the build also runs the integration tests on the default server profile, as described below.

# Run integration tests

The integration tests run with Arquillian against a real server, selected by a Maven profile.
The `wildfly-mojarra4` profile is active by default:

```bash
mvn clean verify
```

To run on a different server, select its profile:

```bash
mvn clean verify -P tomcat-mojarra4
```

Available profiles are `wildfly-mojarra4`, `liberty-myfaces3`, `glassfish-mojarra4`, `tomee-myfaces4`, `tomcat-mojarra4`, `tomcat-mojarra3`, `tomcat-myfaces4`, `tomcat-myfaces3` and `payara-mojarra4`.
CI runs all of them on Java 17.

The tests drive Chrome through Selenium, so Chrome must be installed.
To use a different browser:

```bash
mvn clean verify -Darquillian.browser=firefox
```

To run a single test class:

```bash
mvn clean verify -Dit.test=EagerIT
```

# Java JSON PEP SDK for XACML

This project provides a PEP SDK for Java, that generates a XACML request and response in accordance with the
JSON Profile of XACML 1.1 and 1.0.
The project is organized into a number of submodule projects using Apache Maven. Please see their brief descriptions
below, or see the submodule project's READMEs.

The [`models`](models) can be used to `POST` a request to a Policy Decision Point (such as the Axiomatics Cloud Native PDP),
using an HTTP client of your choice.

The project contains sample clients based on popular HTTP client libraries [Feign](https://github.com/OpenFeign/feign)
and [Jersey](https://github.com/eclipse-ee4j/jersey).

## Project Contents

- [Request and Response Models](models)
- [Client-Core](client-core)
- [Client-Feign](client-feign)
- [Example code using the Feign client](client-feign-example)
- [Client-JaxRs](client-jaxrs)
- [Example code using the JaxRs client](client-jaxrs-example)

More information can be found at [OASIS's JSON Profile of XACML v 1.1](http://docs.oasis-open.org/xacml/xacml-json-http/v1.1/xacml-json-http-v1.1.html).

## How to Use the SDK

See sample code in the [client-feign-example](client-feign-example) and [client-jaxrs-example](client-jaxrs-example) subprojects.

`FeignAuthZClient` and `JaxRsAuthZClient` instances are thread-safe: create one per PDP when your application starts
and share it. `JaxRsAuthZClient` holds HTTP connections, so `close()` it when your application shuts down.

## Building

The SDK requires Java 8 or later. Build and run the tests with the Maven wrapper (no Maven installation needed):

```bash
./mvnw verify
```

## Releasing

Push a tag of the form `v<major>.<minor>.<patch>`, e.g. `v1.0.0`. The [release workflow](.github/workflows/release.yml)
builds and tests that version, publishes it to GitHub Packages, and creates a GitHub release with generated notes.

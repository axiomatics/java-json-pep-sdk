package io.xacml.pep.json.client.jaxrs;

import io.xacml.json.model.Request;
import io.xacml.json.model.Response;
import io.xacml.pep.json.client.AuthZClient;
import io.xacml.pep.json.client.ClientConfiguration;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import java.util.Objects;

import static io.xacml.pep.json.client.PDPConstants.CONTENT_TYPE;

/**
 * Builds a JAX-RS to invoke a Policy Decision Point.
 * It supports both the JSON Profile of XACML 1.0 (where the response could be either an Object or
 * an Array) and the JSON Profile of XACML 1.1 (where the response is always an array - to simplify
 * things)
 * <p>
 * Instances created from a {@link ClientConfiguration} or a {@link WebTarget} are thread-safe and meant to be shared.
 *
 * @author djob
 */
public class JaxRsAuthZClient implements AuthZClient, AutoCloseable {

    /**
     * The authorization service endpoint. A new request is built from it for every call, since
     * {@link Invocation.Builder} instances are mutable and must not be shared between threads.
     */
    protected final WebTarget authorizationServiceTarget;

    /**
     * @deprecated only set by {@link #JaxRsAuthZClient(Invocation.Builder)}. Reusing a single builder for every call
     * is not thread-safe. To set extra HTTP headers, override {@link #newRequest()} instead.
     */
    @Deprecated
    protected final Invocation.Builder requestInvocationBuilder;

    /**
     * The client this instance created itself, and therefore closes in {@link #close()}.
     */
    private final Client ownedClient;

    public JaxRsAuthZClient(WebTarget authorizationServiceTarget) {
        this.authorizationServiceTarget =
                Objects.requireNonNull(authorizationServiceTarget, "Authorization service target must be non-null");
        this.requestInvocationBuilder = null;
        this.ownedClient = null;
    }

    /**
     * @deprecated the given builder is reused for every call, which is not thread-safe.
     * Use {@link #JaxRsAuthZClient(WebTarget)} instead.
     */
    @Deprecated
    public JaxRsAuthZClient(Invocation.Builder requestInvocationBuilder) {
        this.authorizationServiceTarget = null;
        this.requestInvocationBuilder = Objects.requireNonNull(requestInvocationBuilder,
                "Request invocation builder must be non-null");
        this.ownedClient = null;
    }

    public JaxRsAuthZClient(ClientConfiguration clientConfiguration) {

        Objects.requireNonNull(clientConfiguration, "Client configuration must be non-null");
        Objects.requireNonNull(clientConfiguration.getAuthorizationServiceUrl(),
                "Client configuration must contain a non-null authorizationServiceUrl URL");

        Client client = ClientBuilder.newClient();

        // Username (and Password) should be provided if PDP requires Basic Authentication
        if (null != clientConfiguration.getUsername()) {
            Objects.requireNonNull(clientConfiguration.getPassword(),
                    "Client configuration must contain a password when a username is set");
            client.register(HttpAuthenticationFeature.basic(
                    clientConfiguration.getUsername(),
                    clientConfiguration.getPassword())
            );
        }
        this.authorizationServiceTarget = client.target(clientConfiguration.getAuthorizationServiceUrl());
        this.requestInvocationBuilder = null;
        this.ownedClient = client;
    }

    /**
     * Sends the request object to the PDP and returns the response from PDP
     * <p>
     * The Response object is in the format of JSON Profile of XACML 1.1,
     * where the response contains an array of results.
     *
     * @param request the XACML request object
     * @return the response object
     */
    @Override
    public Response makeAuthorizationRequest(Request request) {
        return newRequest().post(Entity.entity(request, CONTENT_TYPE), Response.class);
    }

    /**
     * Creates the HTTP request for a single authorization call. Sub-classes can override it to set extra HTTP headers,
     * e.g. {@code return super.newRequest().header("X-Correlation-Id", id);}
     */
    protected Invocation.Builder newRequest() {
        if (authorizationServiceTarget == null) {
            return requestInvocationBuilder;
        }
        return authorizationServiceTarget.request(CONTENT_TYPE);
    }

    /**
     * Releases the connections of the underlying JAX-RS client, when this instance created it.
     */
    @Override
    public void close() {
        if (ownedClient != null) {
            ownedClient.close();
        }
    }
}

package io.xacml.pep.json.client.jaxrs;

import io.xacml.json.model.Attribute;
import io.xacml.json.model.Category;
import io.xacml.json.model.PDPDecision;
import io.xacml.json.model.Request;
import io.xacml.json.model.Response;
import io.xacml.json.model.Result;
import io.xacml.pep.json.client.ConcurrentCalls;
import io.xacml.pep.json.client.DefaultClientConfiguration;
import io.xacml.pep.json.client.StubPdp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

public class JaxRsAuthZClientTest {

    private StubPdp pdp;

    @Before
    public void startPdp() throws Exception {
        pdp = new StubPdp();
    }

    @After
    public void stopPdp() {
        pdp.close();
    }

    @Test
    public void sendsXacmlJsonRequestWithBasicAuthentication() {
        try (JaxRsAuthZClient client = client("/permit", "enforcer", "secret")) {
            Response response = client.makeAuthorizationRequest(request());

            assertThat(response.getResults().get(0).getDecision(), equalTo(PDPDecision.PERMIT));
        }
        assertThat(pdp.lastHeader("Content-Type"), equalTo("application/xacml+json"));
        assertThat(pdp.lastHeader("Authorization"), equalTo("Basic ZW5mb3JjZXI6c2VjcmV0"));
        assertThat(pdp.lastBody(), containsString("\"Request\""));
        assertThat(pdp.lastBody(), containsString("\"AccessSubject\""));
    }

    @Test
    public void sendsNoCredentialsWithoutUsername() {
        try (JaxRsAuthZClient client = client("/permit", null, null)) {
            client.makeAuthorizationRequest(request());
        }
        assertThat(pdp.lastHeader("Authorization"), nullValue());
    }

    @Test
    public void parsesJsonProfile10Response() {
        try (JaxRsAuthZClient client = client("/deny-1.0", null, null)) {
            Result result = client.makeAuthorizationRequest(request()).getResults().get(0);

            assertThat(result.getDecision(), equalTo(PDPDecision.DENY));
            assertThat(result.getObligations(), hasSize(1));
        }
    }

    @Test(expected = InternalServerErrorException.class)
    public void httpErrorsAreThrown() {
        try (JaxRsAuthZClient client = client("/error", null, null)) {
            client.makeAuthorizationRequest(request());
        }
    }

    @Test(expected = NullPointerException.class)
    public void usernameWithoutPasswordIsRejected() {
        client("/permit", "enforcer", null);
    }

    @Test
    public void sharedClientIsThreadSafe() throws Exception {
        try (JaxRsAuthZClient client = client("/permit", "enforcer", "secret")) {
            assertThat(ConcurrentCalls.run(16, 400,
                    () -> client.makeAuthorizationRequest(request()).getResults().get(0).getDecision() == PDPDecision.PERMIT),
                    empty());
        }
    }

    @Test
    public void subclassesCanAddHeadersPerRequest() {
        DefaultClientConfiguration configuration =
                DefaultClientConfiguration.builder().authorizationServiceUrl(pdp.url("/permit")).build();
        try (JaxRsAuthZClient client = new JaxRsAuthZClient(configuration) {
            @Override
            protected Invocation.Builder newRequest() {
                return super.newRequest().header("X-Correlation-Id", "42");
            }
        }) {
            client.makeAuthorizationRequest(request());
            client.makeAuthorizationRequest(request());
        }
        assertThat(pdp.lastHeader("X-Correlation-Id"), equalTo("42"));
        assertThat(pdp.lastHeaderCount("X-Correlation-Id"), equalTo(1));
        assertThat(pdp.lastHeaderCount("Content-Type"), equalTo(1));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedInvocationBuilderConstructorStillWorks() {
        Client jaxRsClient = ClientBuilder.newClient();
        try {
            Invocation.Builder builder = jaxRsClient.target(pdp.url("/permit")).request("application/xacml+json");
            Response response = new JaxRsAuthZClient(builder).makeAuthorizationRequest(request());

            assertThat(response.getResults().get(0).getDecision(), equalTo(PDPDecision.PERMIT));
        } finally {
            jaxRsClient.close();
        }
    }

    private JaxRsAuthZClient client(String path, String username, String password) {
        return new JaxRsAuthZClient(DefaultClientConfiguration.builder()
                .authorizationServiceUrl(pdp.url(path))
                .username(username)
                .password(password)
                .build());
    }

    private static Request request() {
        Category subject = new Category();
        subject.addAttribute(new Attribute("username", "Alice"));
        Request request = new Request();
        request.addAccessSubjectCategory(subject);
        return request;
    }
}

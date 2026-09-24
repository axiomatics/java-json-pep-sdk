package io.xacml.pep.json.client.feign;

import feign.FeignException;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

public class FeignAuthZClientTest {

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
        Response response = client("/permit", "enforcer", "secret").makeAuthorizationRequest(request());

        assertThat(response.getResults().get(0).getDecision(), equalTo(PDPDecision.PERMIT));
        assertThat(pdp.lastHeader("Content-Type"), equalTo("application/xacml+json"));
        assertThat(pdp.lastHeader("Authorization"), equalTo("Basic ZW5mb3JjZXI6c2VjcmV0"));
        assertThat(pdp.lastBody(), containsString("\"Request\""));
        assertThat(pdp.lastBody(), containsString("\"AccessSubject\""));
    }

    @Test
    public void sendsNoCredentialsWithoutUsername() {
        client("/permit", null, null).makeAuthorizationRequest(request());

        assertThat(pdp.lastHeader("Authorization"), nullValue());
    }

    @Test
    public void parsesJsonProfile10Response() {
        Result result = client("/deny-1.0", null, null).makeAuthorizationRequest(request()).getResults().get(0);

        assertThat(result.getDecision(), equalTo(PDPDecision.DENY));
        assertThat(result.getObligations(), hasSize(1));
    }

    @Test(expected = FeignException.InternalServerError.class)
    public void httpErrorsAreThrown() {
        client("/error", null, null).makeAuthorizationRequest(request());
    }

    @Test(expected = NullPointerException.class)
    public void usernameWithoutPasswordIsRejected() {
        client("/permit", "enforcer", null);
    }

    @Test
    public void sharedClientIsThreadSafe() throws Exception {
        FeignAuthZClient client = client("/permit", "enforcer", "secret");

        assertThat(ConcurrentCalls.run(16, 400,
                () -> client.makeAuthorizationRequest(request()).getResults().get(0).getDecision() == PDPDecision.PERMIT),
                empty());
    }

    private FeignAuthZClient client(String path, String username, String password) {
        return new FeignAuthZClient(DefaultClientConfiguration.builder()
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

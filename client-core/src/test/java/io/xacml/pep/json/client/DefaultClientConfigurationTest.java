package io.xacml.pep.json.client;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class DefaultClientConfigurationTest {

    @Test
    public void toStringDoesNotRevealThePassword() {
        DefaultClientConfiguration configuration = DefaultClientConfiguration.builder()
                .authorizationServiceUrl("https://pdp.example.io/authorize")
                .username("enforcer")
                .password("s3cr3t")
                .build();

        assertThat(configuration.toString(), not(containsString("s3cr3t")));
        assertThat(configuration.toString(), containsString("username=enforcer"));
    }

    @Test
    public void builderToStringDoesNotRevealThePassword() {
        DefaultClientConfiguration.DefaultClientConfigurationBuilder builder = DefaultClientConfiguration.builder()
                .authorizationServiceUrl("https://pdp.example.io/authorize")
                .password("s3cr3t");

        assertThat(builder.toString(), not(containsString("s3cr3t")));
    }

    @Test
    public void authorizationServiceUrlIsUsedAsIs() {
        DefaultClientConfiguration configuration = DefaultClientConfiguration.builder()
                .authorizationServiceUrl("https://gateway.example.io/pdp/v1/decide")
                .build();

        assertThat(configuration.getAuthorizationServiceUrl(), equalTo("https://gateway.example.io/pdp/v1/decide"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void pdpUrlAppendsTheAuthorizeEndpoint() {
        DefaultClientConfiguration configuration = DefaultClientConfiguration.builder()
                .pdpUrl("https://pdp.example.io")
                .build();

        assertThat(configuration.getAuthorizationServiceUrl(), equalTo("https://pdp.example.io/authorize"));
    }

    @Test(expected = IllegalStateException.class)
    public void buildFailsWithoutUrl() {
        DefaultClientConfiguration.builder().build();
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("deprecation")
    public void cannotSetBothUrls() {
        DefaultClientConfiguration.builder().authorizationServiceUrl("https://a").pdpUrl("https://b");
    }
}

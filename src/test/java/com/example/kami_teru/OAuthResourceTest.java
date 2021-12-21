package com.example.kami_teru;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

import com.example.kami_teru.proxies.slack.OAuthResponseData;
import com.example.kami_teru.proxies.slack.TeamData;
import com.example.kami_teru.slack.api.SlackApiMock;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;

public class OAuthResourceTest {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer(new URI(System.getenv("TEST_URL")), "com.example.kami_teru");
        // create the client
        Client c = ClientBuilder.newClient();
        target = c.target(System.getenv("TEST_URL"));
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    /**
     * Test Redirect URL.
     * @throws URISyntaxException
     */
    @Test
    public void testRedirect() throws URISyntaxException {

        HttpServer mockServer = Main.startServer(URI.create(System.getenv("SLACK_URL")), "com.example.kami_teru.slack");
        try {
            final MultivaluedMap<String, String> slackReceives = new MultivaluedHashMap<String, String>();

            SlackApiMock.oautuV2AccessFunc = new Function<MultivaluedMap<String,String>,Response>() {
                @Override
                public Response apply(MultivaluedMap<String, String> formParams) {
                    slackReceives.putAll(formParams);
                    return Response.ok().entity(new OAuthResponseData(){{
                        ok = true;
                        access_token = "OAUTH-TEST-TOKEN";
                        team = new TeamData() {{
                            id = "OAUTH-TEST-TEAM";
                        }};
                    }} ).build();
                }
            };

            Response response = target
                .path("api/oauth")
                .queryParam("code", "xxx")
                .queryParam("state", "")
                .request(MediaType.TEXT_PLAIN)
                .get();

            assertEquals(200, response.getStatus());
            assertEquals("Success", response.readEntity(String.class));
            assertEquals("xxx", slackReceives.getFirst("code"));
           try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
                assertEquals(jedis.get("CLIENT_ID"), slackReceives.getFirst("client_id"));
                assertEquals(jedis.get("CLIENT_SECRET"), slackReceives.getFirst("client_secret"));
                assertEquals("OAUTH-TEST-TOKEN", jedis.get("BOTTOKEN_OAUTH-TEST-TEAM"));
            }
        } finally {
            try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
                jedis.del("BOTTOKEN_OAUTH-TEST-TEAM");
            }
            mockServer.shutdownNow();
        }
    }
}

package com.example.kami_teru;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.example.kami_teru.slack.api.contracts.EventData;
import com.example.kami_teru.slack.api.contracts.EventRequestData;
import com.example.kami_teru.slack.api.contracts.ResponseData;
import com.example.kami_teru.slack.api.controllers.SlackApiControllerMock;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;

public class EventControllerTest {
    
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
     * Test event url verification.
     */
    @Test
    public void testUrlVerification() {
        Response response = target.path("api/event")
            .request()
            .post(Entity.entity(new EventRequestData(){{
                token = "xxx";
                challenge = "yyy";
                type = "url_verification";
            }}, MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());
        assertEquals("yyy", response.readEntity(String.class));
    }

    
    /**
     * Test event callback of `message.im`.
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    @Test
    public void testCallbackMessageIM() throws URISyntaxException, InterruptedException {

        HttpServer mockServer = Main.startServer(URI.create(System.getenv("SLACK_URL")), "com.example.kami_teru.slack");
        try {
            final Map<String, String> slackReceives = new HashMap<String, String>();
            final Semaphore semaphore = new Semaphore(1, true);

            SlackApiControllerMock.postChatMessageFunc = new Function<Map<String,String>,Response>() {
                @Override
                public Response apply(Map<String, String> formParams) {
                    slackReceives.putAll(formParams);
                    semaphore.release();
                    return Response.ok().entity(new ResponseData(){{
                        ok = true;
                    }}).build();
                }
            };

            try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
                jedis.set("BOTTOKEN_OAUTH-TEST-TEAM", "xoxb-xxx");
            }

            semaphore.acquire();

            Response response = target.path("api/event")
                .request()
                .post(Entity.entity(new EventRequestData(){{
                    token = "xxx";
                    team_id = "OAUTH-TEST-TEAM";
                    type = "event_callback";
                    event = new EventData() {{
                        type = "message";
                        channel_type = "im";
                        channel = "ccc";
                        text = "ttt";
                    }};
                }}, MediaType.APPLICATION_JSON));

            assertEquals(200, response.getStatus());

            // Wait async task
            assertEquals(true, semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));            
            assertEquals("ccc", slackReceives.get("channel"));
            assertEquals("ttt て、なんやねん！", slackReceives.get("text"));
            assertEquals("Bearer xoxb-xxx", slackReceives.get("Header_Authorization"));
        } finally {
            try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
                jedis.del("BOTTOKEN_OAUTH-TEST-TEAM");
            }
            mockServer.shutdownNow();
        }
    }

}

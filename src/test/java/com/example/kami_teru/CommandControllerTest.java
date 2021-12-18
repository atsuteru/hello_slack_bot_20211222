package com.example.kami_teru;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.example.kami_teru.businesscard.api.controllers.BusinessCardApiControllerMock;
import com.example.kami_teru.slack.api.contracts.ResponseData;
import com.example.kami_teru.slack.api.controllers.SlackApiControllerMock;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;

public class CommandControllerTest {

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
     * Test to see that the message "Got it!" is sent in the response.
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void testBusinesscard() throws URISyntaxException, InterruptedException, IOException {
        HttpServer mockSlackServer = Main.startServer(URI.create(System.getenv("SLACK_URL")), "com.example.kami_teru.slack");
        try {
            HttpServer mockBisCardGenServer = Main.startServer(URI.create(System.getenv("BUSINESS_CARD_GEN_URL")), "com.example.kami_teru.businesscard");
            try {
                // (1)
                final Map<String, String> slackMessageReceives = new HashMap<String, String>();
                final Semaphore slackMessageSemaphore = new Semaphore(1, true);
                SlackApiControllerMock.responseMessageFunc = new Function<Map<String,String>,Response>() {
                    @Override
                    public Response apply(Map<String, String> formParams) {
                        slackMessageReceives.putAll(formParams);
                        slackMessageSemaphore.release();
                        return Response.ok().entity(new ResponseData(){{
                            ok = true;
                        }}).build();
                    }
                };
                // (2)
                final Map<String, String> businessCardGenReceives = new HashMap<String, String>();
                final Semaphore businessCardGenSemaphore = new Semaphore(1, true);
                BusinessCardApiControllerMock.generateAsPdfFunc = new Function<Map<String,String>,Response>() {
                    @Override
                    public Response apply(Map<String, String> formParams) {
                        businessCardGenReceives.putAll(formParams);
                        businessCardGenSemaphore.release();
                        return Response.ok().entity(new ByteArrayInputStream(new byte[123])).build();
                    }
                };
                // (3)
                final Map<String, Object> slackUploadReceives = new HashMap<String, Object>();
                final Semaphore slackUploadSemaphore = new Semaphore(1, true);
                SlackApiControllerMock.uploadFileFunc = new Function<Map<String,Object>,Response>() {
                    @Override
                    public Response apply(Map<String, Object> formParams) {
                        slackUploadReceives.putAll(formParams);
                        slackUploadSemaphore.release();
                        return Response.ok().entity(new ResponseData(){{
                            ok = true;
                        }}).build();
                    }
                };
    
                try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
                    jedis.set("BOTTOKEN_OAUTH-TEST-TEAM", "xoxb-xxx");
                }
    
                slackMessageSemaphore.acquire();
                businessCardGenSemaphore.acquire();
                slackUploadSemaphore.acquire();
    
                // Test
                Form form = new Form();
                form.param("team_id", "OAUTH-TEST-TEAM");
                form.param("channel_id", "ccc");
                form.param("user_id", "");
                form.param("user_name", "たろう");
                form.param("command", "/Businesscard");
                form.param("text", "太郎,発表者,ピープルソフトウェア株式会社");
                form.param("response_url", System.getenv("SLACK_URL") + "api/responseMessage");
                Response response = target
                    .path("api/command/businesscard")
                    .register(MultiPartFeature.class)
                    .request()
                    .post(Entity.form(form));
                assertEquals(200, response.getStatus());
     
                // Wait slack message posting task
                assertEquals(true, slackMessageSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));            
                assertEquals("たろう さん、名刺の作成を承りました！ お名前=太郎, 役割=発表者, 所属=ピープルソフトウェア株式会社 ですね！", slackMessageReceives.get("text"));

                // Wait business card gen task
                assertEquals(true, businessCardGenSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));    
                assertEquals("templates/business_card.mustache.html", businessCardGenReceives.get("template"));
                assertEquals("太郎", businessCardGenReceives.get("name"));
                assertEquals("発表者", businessCardGenReceives.get("role"));
                assertEquals("ピープルソフトウェア株式会社", businessCardGenReceives.get("company"));

                // Wait slack upload receiving task
                assertEquals(true, slackUploadSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));            
                assertEquals("たろうさんの名刺", slackUploadReceives.get("title"));
                assertEquals("A4マルチカード10面のシートに印刷してご利用ください！", slackUploadReceives.get("initial_comment"));
                assertEquals("pdf", slackUploadReceives.get("filetype"));
                assertEquals("たろうさんの名刺.pdf", slackUploadReceives.get("filename"));
                assertEquals(123, ((InputStream)slackUploadReceives.get("file")).readAllBytes().length);

            } finally {
                mockBisCardGenServer.shutdownNow();;
            }
        } finally {
            try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
                jedis.del("BOTTOKEN_OAUTH-TEST-TEAM");
            }
            mockSlackServer.shutdownNow();
        }
    }
}

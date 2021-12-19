package com.example.kami_teru.proxies.slack;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import redis.clients.jedis.Jedis;

public class SlackClient {

    private static String getSlackUrl() {
        String slackUrl = System.getenv("SLACK_URL");
        if (slackUrl == null || slackUrl.isEmpty()) {
            return "https://slack.com/";
        }
        return slackUrl;
    }

    private static final WebTarget SLACK_API = ClientBuilder.newClient()
        .target(getSlackUrl())
        .path("api");

    private static final WebTarget OAUTH_V2_ACCESS = SLACK_API
        .path("oauth.v2.access");

    private static final WebTarget CHAT_POST_MESSAGE = SLACK_API
        .path("chat.postMessage");

    private static final WebTarget FILES_UPLOAD = SLACK_API
        .path("files.upload")
        .register(MultiPartFeature.class);

    public static Response postOAuthV2Access(String code, String client_id, String client_secret) {
        Form form = new Form();
        form.param("code", code);
        form.param("client_id", client_id);
        form.param("client_secret", client_secret);
        return OAUTH_V2_ACCESS
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.form(form));
    }

    public static Response postMessage(final String access_token, final String channel, final String text) {
        return CHAT_POST_MESSAGE
            .request()
            .header("Authorization", "Bearer " + access_token)
            .post(Entity.entity(new HashMap<String, String>(){{
                put("channel", channel);
                put("text", text);
            }}, MediaType.APPLICATION_JSON));
    }
    public static Response uploadFile(final String team_id, final String channel_id,
        final String title, final String comment,
        final String fileType, final String fileName, final InputStream fileStream) {

        final String access_token;
        try (Jedis jedis = new Jedis(new URI(System.getenv("REDIS_URL")))) {
            access_token = jedis.get("BOTTOKEN_" + team_id);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                String.format("Failed!!(%d)\n%s", Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }

        FormDataMultiPart form = new FormDataMultiPart();
        form.field("channels", channel_id);
        form.field("title", title);
        form.field("filetype", fileType);
        form.field("initial_comment", comment);
        form.bodyPart(new StreamDataBodyPart("file", fileStream, fileName));
        return SlackClient.FILES_UPLOAD
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + access_token)
            .post(Entity.entity(form, form.getMediaType()));
    }

}

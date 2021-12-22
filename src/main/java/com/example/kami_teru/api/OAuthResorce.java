package com.example.kami_teru.api;

import com.example.kami_teru.proxies.redis.RedisClient;
import com.example.kami_teru.proxies.slack.OAuthResponseData;
import com.example.kami_teru.proxies.slack.SlackClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import redis.clients.jedis.Jedis;

@Path("api/oauth")
public class OAuthResorce {
    @GET
    public Response get(@QueryParam("code") String code) {
        final String client_id;
        final String client_secret;
        try (Jedis redis = RedisClient.connect()) {
            client_id = redis.get("CLIENT_ID");
            client_secret = redis.get("CLIENT_SECRET");
        }

        final String access_token;
        final String team_id;
        try (Response oauthResponse = SlackClient.postOAuthV2Access(code, client_id, client_secret)) {
            if (oauthResponse.getStatus() != 200) {
                return Response.status(Status.SERVICE_UNAVAILABLE).entity("Fail: " + oauthResponse.getStatus()).build();
            }
            OAuthResponseData responsData = oauthResponse.readEntity(OAuthResponseData.class);
            if (!responsData.ok) {
                return Response.status(Status.UNAUTHORIZED).entity("Fail: " + responsData.error).build();
            }
            access_token = responsData.access_token;
            team_id = responsData.team.id;
        }

        try (Jedis redis = RedisClient.connect()) {
            redis.set("BOTTOKEN_" + team_id, access_token);
        }
        return Response.ok().entity("Success").build();
    }
}

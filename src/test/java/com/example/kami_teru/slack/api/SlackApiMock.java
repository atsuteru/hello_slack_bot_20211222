package com.example.kami_teru.slack.api;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("api")
public class SlackApiMock {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get() {
        return Response.ok().entity("This is server mock of Slack API.").build();
    }

    public static Function<MultivaluedMap<String, String>, Response> oautuV2AccessFunc;

    @POST
    @Path("oauth.v2.access")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postOAutuV2Access(MultivaluedMap<String, String> requestData) {
        return oautuV2AccessFunc.apply(requestData);
    }

    public static Function<Map<String, String>, Response> postChatMessageFunc;

    @POST
    @Path("chat.postMessage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postChatMessage(Map<String, String> requestData, @HeaderParam("Authorization") String token) {
        requestData.put("Header_Authorization", token);
        return postChatMessageFunc.apply(requestData);        
    }

    public static Function<Map<String, Object>, Response> uploadFileFunc;

    @POST
    @Path("files.upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(
        @FormDataParam("channels") final String channels, 
        @FormDataParam("title") final String title, 
        @FormDataParam("filetype") final String filetype, 
        @FormDataParam("initial_comment") final String initial_comment, 
        @FormDataParam("file") final InputStream file, 
        @FormDataParam("file") final FormDataContentDisposition fileInfo, 
        @HeaderParam("Authorization") final String token) {
        return uploadFileFunc.apply(new HashMap<String, Object>(){{
            put("Header_Authorization", token);
            put("channels", channels);
            put("title", title);
            put("filetype", filetype);
            put("initial_comment", initial_comment);
            put("filename", new String(fileInfo.getFileName().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
            put("file", file);
        }});        
    }

    public static Function<Map<String, String>, Response> responseMessageFunc;

    @POST
    @Path("responseMessage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response responseMessage(Map<String, String> requestData) {
        return responseMessageFunc.apply(requestData);        
    }
}

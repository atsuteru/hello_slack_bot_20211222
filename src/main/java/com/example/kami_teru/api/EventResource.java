package com.example.kami_teru.api;

import com.example.kami_teru.proxies.slack.EventRequestData;
import com.example.kami_teru.tasks.EventCallbackTask;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("api/event")
public class EventResource {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(EventRequestData requestData) {
        if ("event_callback".equals(requestData.type)) {
            new Thread(new EventCallbackTask(requestData)).start();
        }
        return Response.ok().entity(requestData.challenge).build();
    }
}

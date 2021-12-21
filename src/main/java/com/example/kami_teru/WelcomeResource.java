package com.example.kami_teru;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class WelcomeResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get() {
        return Response.ok().entity("Welcome!").build();
    }
}

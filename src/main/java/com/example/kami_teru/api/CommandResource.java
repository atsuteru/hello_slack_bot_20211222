package com.example.kami_teru.api;

import javax.print.attribute.standard.MediaTray;

import com.example.kami_teru.tasks.BusinesscardGenTask;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("api/command")
public class CommandResource {

    @Path("businesscard")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBusinesscard(MultivaluedMap<String, String> requestData) {
        new Thread(new BusinesscardGenTask(requestData)).start();
        return Response.ok().build();
    }
}

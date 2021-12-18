package com.example.kami_teru.businesscard.api.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("api/businesscard")
public class BusinessCardApiControllerMock {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get() {
        return Response.ok().entity("This is server mock of Business card generator API.").build();
    }

    public static Function<Map<String, String>, Response> generateAsPdfFunc;

    @GET
    @Path("generate/as/pdf")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response generateAsPdf(
        @QueryParam("template") final String template, 
        @QueryParam("name") final String name,
        @QueryParam("role") final String role,
        @QueryParam("company") final String company) {
        return generateAsPdfFunc.apply(new HashMap<String, String>(){{
            put("template", template);
            put("name", name);
            put("role", role);
            put("company", company);
        }});
    }
}

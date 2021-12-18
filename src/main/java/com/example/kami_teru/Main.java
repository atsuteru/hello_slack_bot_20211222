package com.example.kami_teru;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Main class.
 *
 */
public class Main {
    public static HttpServer startServer(URI baseUri) {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example.kami_teru package
        final ResourceConfig rc = new ResourceConfig().packages("com.example.kami_teru");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);
    }

    /**
     * Main method.
     * @param args
     * @throws InterruptedException
     * @throws Exception
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final String port = System.getenv("PORT");
        URI baseUri;
        if (port != null && !port.isEmpty()) {
            baseUri = URI.create("https://0.0.0.0:" + port + "/myapp");
        } else {
            baseUri = URI.create("http://localhost:8080/myapp");
        }
        
        final HttpServer server = startServer(baseUri);

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Stopping server..");
                server.shutdownNow();
            }
        }, "Shutdown Hook Thread"));

        server.start();
        System.out.println(String.format("Jersey app started.\nHit CTRL^C to stop it...", baseUri));
        Thread.currentThread().join();
    }
}

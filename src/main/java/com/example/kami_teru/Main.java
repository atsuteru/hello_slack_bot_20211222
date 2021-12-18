package com.example.kami_teru;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Main class.
 *
 */
public class Main {
    public static HttpServer startServer(URI baseUri, String... packages) {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example.kami_teru package
        final ResourceConfig rc = new ResourceConfig()
            .packages(packages)
            .register(MultiPartFeature.class)
            .register(new LoggingFeature(Logger.getLogger(baseUri.toString()), Level.OFF, LoggingFeature.Verbosity.PAYLOAD_TEXT, 8192));

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);
    }

    /**
     * Main method.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        final String port = System.getenv("PORT");
        URI baseUri;
        if (port != null && !port.isEmpty()) {
            baseUri = URI.create("https://0.0.0.0:" + port);
        } else {
            baseUri = URI.create("http://localhost:8080/");
        }
        
        final HttpServer server = startServer(baseUri, "com.example.kami_teru");

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

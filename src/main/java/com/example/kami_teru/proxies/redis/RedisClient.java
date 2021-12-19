package com.example.kami_teru.proxies.redis;

import java.net.URI;
import java.net.URISyntaxException;

import redis.clients.jedis.Jedis;

public class RedisClient {
    private static String getRedisURL() {
        return System.getenv("REDIS_URL");
    }

    public static Jedis connect() {
        try {
            return new Jedis(new URI(getRedisURL()));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Redis settings are incorrect.", e);
        }
    }

}

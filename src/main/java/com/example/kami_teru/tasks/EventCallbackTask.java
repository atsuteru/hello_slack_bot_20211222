package com.example.kami_teru.tasks;

import com.example.kami_teru.proxies.redis.RedisClient;
import com.example.kami_teru.proxies.slack.EventRequestData;
import com.example.kami_teru.proxies.slack.SlackClient;

import redis.clients.jedis.Jedis;

public class EventCallbackTask implements Runnable{

    private final EventRequestData requestData;

    public EventCallbackTask(EventRequestData requestData) {
        this.requestData = requestData;
    }

    @Override
    public void run() {
        if (requestData.event.bot_id != null && !requestData.event.bot_id.isEmpty()) {
            return;
        }

        final String access_token;
        try (Jedis redis = RedisClient.connect()) {
            access_token = redis.get("BOTTOKEN_" + requestData.team_id);
        }
        SlackClient.postMessage(access_token, 
                requestData.event.channel, 
                requestData.event.text + " て、なんやねん！")
                .close();
    }
}

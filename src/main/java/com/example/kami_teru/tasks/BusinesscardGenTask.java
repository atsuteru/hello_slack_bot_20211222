package com.example.kami_teru.tasks;

import java.io.InputStream;

import com.example.kami_teru.proxies.businesscard.BusinessCardGeneratorClient;
import com.example.kami_teru.proxies.slack.ResponseData;
import com.example.kami_teru.proxies.slack.SlackClient;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class BusinesscardGenTask implements Runnable {

    private final MultivaluedMap<String, String> requestData;
    public BusinesscardGenTask(MultivaluedMap<String, String> requestData) {
        this.requestData = requestData;
    }

    @Override
    public void run() {
        final String text = requestData.getFirst("text");
        final String[] parameters = text.split(",");
        final String response_url = requestData.getFirst("response_url");
        if (parameters.length != 3) {
            SlackClient.responseMessage(response_url, 
                    String.format("パラメーターが違いますよ！ /Businesscard %s", text));
            return;
        }
        final String name = parameters[0];
        final String role = parameters[1];
        final String organization = parameters[2];
        final String user_name = requestData.getFirst("user_name");

        SlackClient.responseMessage(response_url,
                String.format(
                        "%s さん、名刺の作成を承りました！ お名前=%s, 役割=%s, 所属=%s ですね！",
                        user_name, name, role, organization));

        final String team_id = requestData.getFirst("team_id");
        final String channel_id = requestData.getFirst("channel_id");

        try (Response genResponse = BusinessCardGeneratorClient.generate(
            "https://hello-slack-bot-20211222.web.app/certificate_of_participation.mustache.html", 
            name, role, organization)) {

            if (genResponse.getStatus() != 200) {
                SlackClient.responseMessage(response_url,
                        String.format("名刺の作成に失敗しました。。。(%d)", genResponse.getStatus()));
                return;
            }

            try (Response uploadResponse = SlackClient.uploadFile(
                team_id, channel_id, 
                user_name + "さんの名刺",
                "A4マルチカード10面のシートに印刷してご利用ください！",
                "pdf",
                user_name + "さんの名刺.pdf",
                genResponse.readEntity(InputStream.class))) {

                if (uploadResponse.getStatus() != 200) {
                    SlackClient.responseMessage(response_url,
                            String.format("名刺のお届けに失敗しました。。。(%d)", uploadResponse.getStatus()));
                    return;
                }
                ResponseData result = uploadResponse.readEntity(ResponseData.class);
                if (!result.ok) {
                    SlackClient.responseMessage(response_url,
                            String.format("名刺のお届けに失敗しました。。。(%s)", result.error));
                    return;
                }
            }        
        }
    }
}

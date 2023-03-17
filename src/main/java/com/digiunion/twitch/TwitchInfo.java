package com.digiunion.twitch;

import java.util.Arrays;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public class TwitchInfo {

    private final static Dotenv dotenv = Dotenv.configure()
    .directory("src\\main\\java\\com\\digiunion\\twitch")
    .filename("Twitch.env")
    .load();

    public record Info(String token, String channelName, String clientId, String clientSecret, String redirectUrl, List<String> scopes){}
    
    static Info info = new Info(dotenv.get("TOKEN"), dotenv.get("CHANNEL_NAME"),dotenv.get("CLIENT_ID"), dotenv.get("CLIENT_SECRET"), dotenv.get("REDIRECT_URL"), Arrays.asList(dotenv.get("SCOPES").replaceAll("[\\[\\]\']", "").split(",")));
}

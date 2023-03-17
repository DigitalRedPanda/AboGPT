package com.digiunion.twitch;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digiunion.twitch.TwitchInfo.Info;
import com.digiunion.websocket.ClientWebsocket;

import jakarta.websocket.DeploymentException;

public class TwitchClient {

  private final String twitchApi = "https://api.twitch.tv/";
  
  private final String twitchId =  "https://id.twitch.tv/";

  private final String twitchIRC = "wss://irc-ws.chat.twitch.tv:443";

  private final Logger logger = LoggerFactory.getLogger(TwitchClient.class);

  public final HttpClient client = HttpClient.newHttpClient();

  public static Info info = TwitchInfo.info;

  public CompletableFuture<Boolean> validateToken()
      throws URISyntaxException, IOException, InterruptedException {
    return CompletableFuture.completedFuture(client.send(HttpRequest.newBuilder()
    .GET().uri(new URI(twitchId + "oauth2/validate"))
        .header("Authorization",
            "OAuth " + info.token())
        .build(), BodyHandlers.ofString()).statusCode() == 200);

  }
  public CompletableFuture<String> getToken()
      throws URISyntaxException, IOException, InterruptedException, ExecutionException {
    return validateToken().thenApply(isTokenValid -> isTokenValid ? info.token():null);
  }

  private String fetchToken() throws IOException, InterruptedException{
    // Unfinished
    client.send(HttpRequest.newBuilder().POST(BodyPublishers.noBody()).uri(URI.create(
      "https://id.twitch.tv/oauth2/token?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s".formatted(info.clientId(),info.clientSecret(),null,info.redirectUrl())
)).build(), BodyHandlers.ofString()).body();
    return null;
  }

  public CompletableFuture<Long> getUserIdByName(String name) throws URISyntaxException {
  
      return client.sendAsync(HttpRequest.newBuilder().uri(URI.create(twitchApi + "helix/users?login=" + name))
      .GET()
      .headers("Authorization", "Bearer " + info.token(), "Client-Id", info.clientId())
      .build(), BodyHandlers.ofString())
          .thenApply(response ->{
            //System.out.println("user id: "+ response.body());
            return new JSONObject(response.body()).getJSONArray("data").getJSONObject(0).getLong("id");});
    
  }

  public CompletableFuture<Long> getGameId(String gameName) throws URISyntaxException{
    return client.sendAsync(HttpRequest.newBuilder().GET()
    .headers("Authorization", "Bearer "+ info.token(), "Client-Id", info.clientId())
    .uri(new URI(twitchApi + "helix/games?name=" + gameName.replace(" ", "%20")))
    .build(), BodyHandlers.ofString()).thenApply(response -> {
        //System.out.println("game id: " + response.body());
      return new JSONObject(response.body()).getJSONArray("data").getJSONObject(0).getLong("id");
    }
    );
  }

  public CompletableFuture<Void> setCategory(String category, String broadcasterName) throws URISyntaxException, InterruptedException, ExecutionException{
    var future = getGameId(category);
    var future2 = getUserIdByName(broadcasterName);
    return CompletableFuture.allOf(future, future2).thenAccept(some -> 
    {
      try {
        client.send(HttpRequest.newBuilder().headers("Authorization", "Bearer " + info.token(),
        "Client-Id", info.clientId(),
        "Content-Type", "application/json")
        .uri(URI.create(twitchApi + "helix/channels?broadcaster_id=" + future2.join()))
        .method("PATCH", BodyPublishers.ofString("""
          {
            \"game_id\": %s 
          }
            """.formatted(future.join()))).build(), BodyHandlers.ofString());
            //System.out.println(request.body());
      } catch (IOException | InterruptedException e) {
        logger.error("Failed to set category; {}", e.getMessage());
      }
    });
  }

  public CompletableFuture<Void> unsetCategory(String broadcasterName) throws URISyntaxException{
    return getUserIdByName(broadcasterName).thenAccept(id -> {
      try {
        client.send(HttpRequest.newBuilder().headers("Authorization", "Bearer " + info.token(),
        "Client-Id", info.clientId(),
        "Content-Type", "application/json")
        .uri(URI.create(twitchApi + "helix/channels?broadcaster_id=" + id))
        .method("PATCH", BodyPublishers.ofString("""
          {
            \"game_id\": 0 
          }
            """)).build(), BodyHandlers.discarding());
      } catch (IOException | InterruptedException e) {
        logger.error("Could not unset category on {}, {}", broadcasterName, e.getMessage());
      }
    });
  }

  public CompletableFuture<String> subscribe(String event, long channelId){
    // Unfinished
    return client.sendAsync(HttpRequest.newBuilder().headers("Authorization", info.token(),
    "Client-Id",info.clientId(),
    "Content-Type", "application/json").POST(
      BodyPublishers.ofString(
      """
      \"type\": 
      """)).build(), BodyHandlers.ofString()).thenApply(HttpResponse::body);
  

  }


  public void init() throws URISyntaxException, IOException, InterruptedException, ExecutionException, DeploymentException {
        try(var connection = ClientManager.createClient().connectToServer(new ClientWebsocket(), URI.create("wss://irc-ws.chat.twitch.tv:443"));
        ) {
          while(connection.isOpen()){
          }
      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    
  }}
}
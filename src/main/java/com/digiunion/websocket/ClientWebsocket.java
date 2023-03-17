package com.digiunion.websocket;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.glassfish.grizzly.threadpool.FixedThreadPool;
import org.glassfish.tyrus.core.ExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digiunion.morsecode.MorseCoder;
import com.digiunion.twitch.TwitchClient;
import com.digiunion.twitch.TwitchInfo.Info;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import lombok.Getter;

@ClientEndpoint 
public class ClientWebsocket {


    private Info info = TwitchClient.info;

    private TwitchClient tclient;

    private ExecutorService threadPool;
    
    @Getter
    private Session session;

    private Logger logger = LoggerFactory.getLogger(ClientEndpoint.class);

    public ClientWebsocket() {
        threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        tclient = new TwitchClient();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("Connection with id {} has been established", session.getId());
        CompletableFuture.runAsync(() -> {
        try {
            session.getBasicRemote().sendText("PASS oauth:" + info.token());
            logger.info("OAuth passed");
            session.getBasicRemote().sendText("NICK "+ info.channelName());
            logger.info("Nickname passed");
        } catch (IOException e) {
            logger.error("Could not pass oauth and nickname to twtich; {}", e.getMessage());
        }}, threadPool).thenRun(()-> {
            joinChannel(info.channelName());
        });
    }

    public void joinChannel(String channelName){
        CompletableFuture.runAsync(() -> { 
        try {
            session.getBasicRemote().sendText("JOIN #" + channelName);
        } catch (IOException e) {
            logger.error("Could not join {} message: {}", channelName, e.getMessage());
        }
        }, threadPool).thenRun(() -> {
            try {
                sendMessage("PRIVMSG #%s :AINTNOWAY I'm here".formatted(channelName));
            } catch (IOException e) {
                logger.error("Could not send join channel message");
            }
            });
    
    }

    @OnMessage
    public void OnMessage(String message){
        CompletableFuture.runAsync(()->{
            var message1 = message.trim();
            if(message1.contains("PING")){
                logger.info(message1);
                try {
                    sendMessage("PONG :tmi.twitch.tv");
                } catch (IOException e) {
                    logger.error("Could not ping TwitchIRC");
                }
            }
            else if(message1.contains("PRIVMSG")){
                var parts =  message1.replaceFirst("([!@]\\w+){2}\\.tmi\\.twitch\\.tv", "")
                .substring(1)
                .split(":");
                var context = parts[0].split(" ");
                var channel = context[2].substring(1);
                var user = context[0];
                var content = parts[1].split(" ");
                System.out.println(content[0] + " " + user);
                if(content[0].charAt(0) == '!'){
                    var commandArgs = content;
                    var command = commandArgs[0].substring(1);
                    logger.info("[{}] {}: {}", channel, user, command);
                    if(commandArgs.length >= 2)
                        switch(command){
                            case "join":
                                if(user.equals("digital_red_panda"))
                                    joinChannel(commandArgs[1]);
                                else
                                    reply(user, channel, "classicGhosty");
                                break;
                            case "leave":
                                if(user.equals("digital_red_panda"))
                                    leaveChannel(channel);
                                else
                                    reply(user, channel, "classicGhosty");
                                break;
                            case "morsecode":
                                reply(user, channel, MorseCoder.code(Arrays.stream(commandArgs)
                                .skip(1)
                                .collect(Collectors.joining(" "))));
                                break;
                            case "setgame":
                                var game = Arrays.stream(commandArgs)
                                .skip(1)
                                .collect(Collectors.joining(" "));
                                try {
                                    tclient.setCategory(game,channel)
                                    .thenAccept(nothing -> reply(user, channel, "game has been set to " + game)).join();
                                } catch (URISyntaxException | InterruptedException | ExecutionException e) {
                                    logger.error("[{}] {} failed to set category to {}; {}", channel, user, game);
                                }
                                break;
                            default: reply(user, channel, "Donki ?");
                    }
                    else if (commandArgs.length == 1){
                        switch(command){
                            case "unsetgame":
                                try {
                                    tclient.unsetCategory(channel)
                                    .thenAccept(nothing -> reply(user, channel, "game has been unset"));
                                } catch (URISyntaxException e) {
                                    logger.error("could not unset category on {}", channel);
                                }
                                break;
                        }
                    }
                    else 
                        reply(user, channel, "classicGhosty");
                    return;
                }
                logger.info("[{}] {}: {}", channel, user, parts[1]);

            }
            
        }, threadPool);
    }

    public void leaveChannel(String channel){
        try {
            sendMessage("PART #%s :@%s deadass".formatted(channel,channel));
        } catch (IOException e) {
            logger.error("Failed to leave {}", channel);
        }
    }


    public void reply(String user, String channel, String content){
        sendChannelMessage(channel, "@%s, %s".formatted(user, content));
    }

    public void sendChannelMessage(String channel, String content){
        try {
            sendMessage("PRIVMSG #%s :%s".formatted(channel, content));
        } catch (IOException e) {
            logger.error("Could not send \"{}\" to {}", content, channel);
        }
    }

    public void sendMessage(String message) throws IOException{
            session.getBasicRemote().sendText(message);
            logger.info(message);
        
}

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session {} has encountered an error: {}", throwable.getMessage());
    }

    @OnClose
    public void onClose(CloseReason reason){
        logger.warn("Connection closed reason: {}", reason.toString());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
}
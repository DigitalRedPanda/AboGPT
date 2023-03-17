package com.digiunion;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digiunion.twitch.TwitchClient;

import jakarta.websocket.DeploymentException;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {
            try {
                new TwitchClient().init();
            } catch (IOException | DeploymentException | URISyntaxException | InterruptedException
                    | ExecutionException e) {
                e.printStackTrace();
            }
    }
}

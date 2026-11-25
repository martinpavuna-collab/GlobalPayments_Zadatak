package main.java;


import main.java.client.MessageClient;
import main.java.common.protocol.FrameReader;
import main.java.common.protocol.FrameWriter;
import main.java.common.protocol.IReader;
import main.java.common.protocol.IWriter;
import main.java.config.AppConfig;

import java.io.IOException;

public class ClientMain {

    public static void main(String[] args) {
        int port;

        try {
            port = AppConfig.getPort(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Configuration error: " + e.getMessage());
            return;
        }


        IReader reader = new FrameReader();
        IWriter writer = new FrameWriter();

        MessageClient client =
                new MessageClient("localhost", AppConfig.SERVER_PORT, reader, writer);

        try {
            client.sendFile(AppConfig.INPUT_FILE);
        } catch (IOException e) {
            System.err.println("Client failed: " + e.getMessage());
        }

    }
}
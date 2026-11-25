package com.globalpayments.task;


import com.globalpayments.task.client.MessageClient;
import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.FrameWriter;
import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.common.protocol.IWriter;
import com.globalpayments.task.config.AppConfig;

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
package com.globalpayments.task;

import com.globalpayments.task.client.MessageClient;
import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.FrameWriter;
import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.common.protocol.IWriter;
import com.globalpayments.task.config.AppConfig;

import java.io.IOException;

/** Client. Usage: ClientMain [port] (default port: see AppConfig). */
public class ClientMain {

    public static void main(String[] args) {
        int port;

        try {
            port = AppConfig.getPort(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }

        String inputFile = AppConfig.INPUT_FILE;
        String host = AppConfig.HOST;

        IReader reader = new FrameReader();
        IWriter writer = new FrameWriter();

        MessageClient client = new MessageClient(host, port, reader, writer);

        try {
            client.sendFile(inputFile);
        } catch (IOException e) {
            System.err.println("Client failed: " + e.getMessage());
            System.exit(2);
        }
    }
}

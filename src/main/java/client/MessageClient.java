package main.java.client;

import main.java.common.protocol.IReader;
import main.java.common.protocol.IWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MessageClient {

    private final String host;
    private final int port;
    private final IReader reader;
    private final IWriter writer;

    public MessageClient(String host, int port,
                         IReader reader, IWriter writer) {
        this.host = host;
        this.port = port;
        this.reader = reader;
        this.writer = writer;
    }

    public void sendFile(String filePath) throws IOException {

        try (
                FileInputStream fileInputStream = new FileInputStream(filePath);
                Socket socket = new Socket(host, port)
        ) {
            InputStream inputStream = fileInputStream;
            OutputStream outputStream = socket.getOutputStream();

            byte[] payload;

            while ((payload = reader.readFrame(inputStream)) != null) {
                writer.writeFrame(outputStream, payload);
            }

            System.out.println("File successfully sent.");
        }
    }
}
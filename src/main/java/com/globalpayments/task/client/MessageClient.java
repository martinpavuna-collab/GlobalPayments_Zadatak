package com.globalpayments.task.client;

import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.common.protocol.IWriter;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MessageClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

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

    /**
     * Reads the file frame by frame and sends every frame to the server, in order.
     * If the file is malformed or truncated, the frames before the problem are still sent and
     * an IOException then describes the problem.
     *
     * @return the number of frames sent
     */
    public int sendFile(String filePath) throws IOException {

        try (
                InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
                Socket socket = connect()
        ) {
            OutputStream outputStream = socket.getOutputStream();

            int sent = 0;

            while (true) {
                byte[] payload;
                try {
                    payload = reader.readFrame(inputStream);
                } catch (IOException e) {
                    throw new IOException("Malformed input file '" + filePath + "' (frame #" + (sent + 1)
                            + "): " + e.getMessage() + " " + sent + " valid frame(s) were sent before the error.", e);
                }

                if (payload == null) {
                    break; // EOF right after a complete frame
                }

                writer.writeFrame(outputStream, payload);
                sent++;
            }

            System.out.println(sent + " frame(s) successfully sent.");
            return sent;
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw new IOException("Cannot connect to " + host + ":" + port + " (" + e.getMessage() + ")", e);
        }
    }
}

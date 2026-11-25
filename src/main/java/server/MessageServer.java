package main.java.server;

import main.java.common.protocol.IReader;
import main.java.config.AppConfig;
import main.java.database.IDatabaseManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Main server class.
 */
public class MessageServer {

    private final int port;
    private final IReader reader;
    private final IDatabaseManager databaseManager;

    public MessageServer(int port, IReader reader,  IDatabaseManager databaseManager) {
        this.port = port;
        this.reader = reader;
        this.databaseManager = databaseManager;
    }

    public void start() throws IOException {
        Path outputPath = Paths.get(AppConfig.OUTPUT_FILE);

        Files.createDirectories(outputPath.getParent());

        try (
                ServerSocket serverSocket = new ServerSocket(port);
                BufferedWriter fileWriter = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(outputPath.toFile(), false),
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            System.out.println("Server listening on port " + port);

            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Client connected!");

                InputStream inputStream = clientSocket.getInputStream();
                byte[] payload;

                while ((payload = reader.readFrame(inputStream)) != null) {
                    String message = new String(
                            payload,
                            Charset.forName(AppConfig.INPUT_CHARSET)
                    );

                    fileWriter.write(message);
                    fileWriter.newLine();
                    databaseManager.saveMessage(message);
                }
            }  catch (IOException e) {
                System.err.println("Invalid frame or communication error: "
                        + e.getMessage());
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }

    }
}

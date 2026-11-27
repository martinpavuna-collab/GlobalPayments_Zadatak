package com.globalpayments.task.server;

import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.config.AppConfig;
import com.globalpayments.task.database.IDatabaseManager;
import com.globalpayments.task.model.InvalidRecordException;
import com.globalpayments.task.model.TransactionParser;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Main server class.
 *
 * Reads frames, decodes the payload from ISO-8859-2, appends every message as a UTF-8 line
 * to the output file and stores parsed transactions in the database.
 *
 * Clients are served one after another. A bad connection (invalid frame, timeout, I/O or database
 * error) is closed and reported; the server keeps listening for the next client.
 */
public class MessageServer {

    /** Outcome of one connection. */
    public static final class Result {

        private final int frames;
        private final int stored;
        private final int skipped;
        private final String error;

        /**
         * @param frames  frames read successfully (all of them were written to the output file)
         * @param stored  transactions inserted into the database
         * @param skipped messages not stored in the database (end markers such as END/KRAJ, or malformed records)
         * @param error   why the connection was aborted, or null if it ended cleanly
         */
        public Result(int frames, int stored, int skipped, String error) {
            this.frames = frames;
            this.stored = stored;
            this.skipped = skipped;
            this.error = error;
        }

        public int frames() {
            return frames;
        }

        public int stored() {
            return stored;
        }

        public int skipped() {
            return skipped;
        }

        public String error() {
            return error;
        }

        public boolean ok() {
            return error == null;
        }
    }

    private static final Charset INPUT_CHARSET = Charset.forName(AppConfig.INPUT_CHARSET);

    private final int port;
    private final String outputFile;
    private final IReader reader;
    private final IDatabaseManager databaseManager;

    private ServerSocket serverSocket;
    private BufferedWriter fileWriter;
    private volatile boolean stopped;

    public MessageServer(int port, IReader reader, IDatabaseManager databaseManager) {
        this(port, AppConfig.OUTPUT_FILE, reader, databaseManager);
    }

    public MessageServer(int port, String outputFile, IReader reader, IDatabaseManager databaseManager) {
        this.port = port;
        this.outputFile = outputFile;
        this.reader = reader;
        this.databaseManager = databaseManager;
    }

    /** Opens the port and the output file, then serves clients until {@link #stop()} is called. */
    public void start() throws IOException {
        bind();
        try {
            serve();
        } finally {
            stop();
        }
    }

    /** Opens the port (0 = any free port) and the output file (appending to it). */
    public void bind() throws IOException {
        Path outputPath = Paths.get(outputFile).toAbsolutePath();
        if (outputPath.getParent() != null) {
            java.nio.file.Files.createDirectories(outputPath.getParent());
        }

        fileWriter = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outputPath.toFile(), true),
                        StandardCharsets.UTF_8
                )
        );
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            fileWriter.close();
            throw e;
        }
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /** Accepts clients one after another until {@link #stop()} is called. */
    public void serve() {
        System.out.println("Server listening on port " + getPort());

        while (!stopped) {
            try {
                serveOne();
            } catch (IOException e) {
                if (!stopped) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            } catch (RuntimeException e) {
                // an unexpected bug while handling one client must not stop the whole server
                System.err.println("Unexpected error, connection dropped: " + e);
            }
        }
    }

    /** Accepts exactly one client and processes all of its frames. */
    public Result serveOne() throws IOException {
        try (Socket clientSocket = serverSocket.accept()) {
            clientSocket.setSoTimeout(AppConfig.READ_TIMEOUT_MS);
            String client = String.valueOf(clientSocket.getRemoteSocketAddress());
            System.out.println("Client connected: " + client);

            // Buffered once per connection: the frame reader reads byte by byte.
            Result result = process(new BufferedInputStream(clientSocket.getInputStream()), client);

            System.out.println("Client " + client + " done: " + result.frames() + " frame(s), "
                    + result.stored() + " stored in DB, " + result.skipped() + " not stored in DB");
            return result;
        }
    }

    /** Processes all frames of one stream. Bad input is reported in the result, never thrown. */
    public Result process(InputStream inputStream, String client) {
        int frames = 0;
        int stored = 0;
        int skipped = 0;

        while (true) {
            byte[] payload;
            try {
                payload = reader.readFrame(inputStream);
            } catch (SocketTimeoutException e) {
                return aborted(frames, stored, skipped, client,
                        "Read timed out after " + AppConfig.READ_TIMEOUT_MS + " ms");
            } catch (IOException e) {
                return aborted(frames, stored, skipped, client,
                        "Invalid frame or communication error (frame #" + (frames + 1) + "): " + e.getMessage());
            }

            if (payload == null) {
                return new Result(frames, stored, skipped, null); // clean end of stream
            }
            String message = new String(payload, INPUT_CHARSET);

            // "One message per line" cannot hold a message that itself contains a line break.
            if (message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0) {
                return aborted(frames, stored, skipped, client, "Invalid frame #" + (frames + 1)
                        + ": payload contains a line break and cannot be stored as one line");
            }
            frames++;

            try {
                fileWriter.write(message);
                fileWriter.newLine();
                fileWriter.flush(); // keep the file current: the server normally never closes it itself
            } catch (IOException e) {
                return aborted(frames, stored, skipped, client, "Cannot write to output file: " + e.getMessage());
            }

            if (TransactionParser.isEndMarker(message)) {
                skipped++;
                continue;
            }

            try {
                databaseManager.saveTransaction(TransactionParser.parse(message));
                stored++;
            } catch (InvalidRecordException e) {
                skipped++;
                System.err.println("Frame #" + frames + " from " + client
                        + " written to file but not stored in DB: " + e.getMessage());
            } catch (SQLException e) {
                return aborted(frames, stored, skipped, client, "Database error: " + e.getMessage());
            }
        }
    }

    private static Result aborted(int frames, int stored, int skipped, String client, String error) {
        System.err.println("Connection " + client + " aborted after " + frames + " valid frame(s): " + error);
        return new Result(frames, stored, skipped, error);
    }

    /** Stops serving and closes the port and the output file. */
    public void stop() {
        stopped = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // closing
        }
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
        } catch (IOException ignored) {
            // closing
        }
    }
}

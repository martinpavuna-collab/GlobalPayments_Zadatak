package com.globalpayments.task.integration;

import com.globalpayments.task.client.MessageClient;
import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.FrameWriter;
import com.globalpayments.task.config.AppConfig;
import com.globalpayments.task.database.FakeDatabaseManager;
import com.globalpayments.task.server.MessageServer;
import com.globalpayments.task.server.MessageServer.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real client -> TCP -> server -> output file + SQLite, on an ephemeral port. */
class ClientServerIntegrationTest {

    private static final String HOST = "127.0.0.1";

    @TempDir
    Path dir;

    private Path outputFile;
    private FakeDatabaseManager databaseManager;
    private MessageServer server;
    private ExecutorService executor;

    @BeforeEach
    void startServer() throws Exception {
        outputFile = dir.resolve("out").resolve("messages.txt");
        databaseManager = new FakeDatabaseManager();
        server = new MessageServer(0, outputFile.toString(), new FrameReader(), databaseManager);
        server.bind(); // port 0 = any free port
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void stopServer() throws Exception {
        executor.shutdownNow();
        server.stop();
    }

    // ---------- helpers ----------

    /** Builds a framed message: 2-byte big-endian length + ISO-8859-2 payload. */
    private static byte[] frame(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new FrameWriter().writeFrame(out, text.getBytes(Charset.forName(AppConfig.INPUT_CHARSET)));
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }

    private Path inputFile(byte[] content) throws IOException {
        Path file = dir.resolve("input.bin");
        Files.write(file, content);
        return file;
    }

    private MessageClient client() {
        return new MessageClient(HOST, server.getPort(), new FrameReader(), new FrameWriter());
    }

    private Future<Result> acceptOneConnection() {
        return executor.submit(server::serveOne);
    }

    private Result await(Future<Result> future) throws Exception {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new AssertionError("server failed", e.getCause());
        }
    }

    private Result sendFileAndAwait(Path input) throws Exception {
        Future<Result> summary = acceptOneConnection();
        client().sendFile(input.toString());
        return await(summary);
    }

    private Result sendRawAndAwait(byte[] bytes) throws Exception {
        Future<Result> summary = acceptOneConnection();
        try (Socket socket = new Socket(HOST, server.getPort())) {
            OutputStream out = socket.getOutputStream();
            out.write(bytes);
            out.flush();
        }
        return await(summary);
    }

    private List<String> outputLines() throws IOException {
        return Files.readAllLines(outputFile, StandardCharsets.UTF_8);
    }

    private void awaitLines(int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(outputFile) && outputLines().size() >= expected) {
                return;
            }
            Thread.sleep(20);
        }
        fail("server did not write " + expected + " line(s) in time");
    }

    /** What the server handed to the (fake) database, as {@code type|id|amount|currency}. */
    private List<String> dbRows() {
        return databaseManager.savedAsLines();
    }

    // ---------- sample 1: ASCII ----------

    @Test
    void sample1AsciiMessagesAreWrittenToFileAndDatabase() throws Exception {
        Path input = inputFile(concat(
                frame("PAYMENT|12345|100.50|HRK"), frame("REFUND|98765|42.00|EUR"), frame("END")));

        Result summary = sendFileAndAwait(input);

        assertTrue(summary.ok());
        assertEquals(3, summary.frames());
        assertEquals(2, summary.stored());
        assertEquals(List.of("PAYMENT|12345|100.50|HRK", "REFUND|98765|42.00|EUR", "END"), outputLines());
        assertEquals(List.of("PAYMENT|12345|100.50|HRK", "REFUND|98765|42.00|EUR"), dbRows());
    }

    // ---------- sample 2: Croatian characters ----------

    @Test
    void sample2CroatianCharactersAreConvertedFromIso88592ToUtf8() throws Exception {
        Path input = inputFile(concat(
                frame("UPLATA|ČĆŽŠĐ|100.50|HRK"), frame("STORNO|žćčšđ|42.00|EUR"), frame("KRAJ")));

        sendFileAndAwait(input);

        assertEquals(List.of("UPLATA|ČĆŽŠĐ|100.50|HRK", "STORNO|žćčšđ|42.00|EUR", "KRAJ"), outputLines());
        assertEquals(List.of("UPLATA|ČĆŽŠĐ|100.50|HRK", "STORNO|žćčšđ|42.00|EUR"), dbRows());

        // The file must really be UTF-8 ('Č' = C4 8C), not the incoming ISO-8859-2 byte (C8).
        String raw = Files.readString(outputFile, StandardCharsets.UTF_8);
        String nl = System.lineSeparator(); // the server writes the platform line separator
        assertEquals("UPLATA|ČĆŽŠĐ|100.50|HRK" + nl + "STORNO|žćčšđ|42.00|EUR" + nl + "KRAJ" + nl, raw);
        byte[] bytes = Files.readAllBytes(outputFile);
        assertEquals((byte) 0xC4, bytes[7]);
        assertEquals((byte) 0x8C, bytes[8]);
    }

    @Test
    void providedSampleFilesProduceTheExpectedOutput() throws Exception {
        sendFileAndAwait(Path.of("input", "sample-input-ascii.bin"));
        sendFileAndAwait(Path.of("input", "sample-input-hr.bin"));

        assertEquals(List.of(
                "PAYMENT|12345|100.50|HRK", "REFUND|98765|42.00|EUR", "END",
                "UPLATA|ČĆŽŠĐ|100.50|HRK", "STORNO|žćčšđ|42.00|EUR", "KRAJ"), outputLines());
    }

    // ---------- error handling ----------

    @Test
    void clientReportsTruncatedPayloadAndStillDeliversEarlierFrames() throws Exception {
        byte[] truncated = {0x00, 0x05, 'A', 'B', 'C'};
        Path input = inputFile(concat(frame("PAYMENT|1|1.00|EUR"), truncated));

        Future<Result> summary = acceptOneConnection();
        IOException e = assertThrows(IOException.class,
                () -> client().sendFile(input.toString()));

        assertTrue(e.getMessage().contains("incomplete payload"));
        assertTrue(e.getMessage().contains("1 valid frame(s) were sent"));
        assertEquals(1, await(summary).frames());
        assertEquals(List.of("PAYMENT|1|1.00|EUR"), outputLines());
    }

    @Test
    void clientReportsZeroLengthFrameInInput() throws Exception {
        Path input = inputFile(new byte[]{0x00, 0x00});

        acceptOneConnection();
        IOException e = assertThrows(IOException.class,
                () -> client().sendFile(input.toString()));

        assertTrue(e.getMessage().contains("Invalid frame length: 0"));
    }

    @Test
    void clientReportsMissingServer() throws Exception {
        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        Path input = inputFile(frame("END"));

        IOException e = assertThrows(IOException.class,
                () -> new MessageClient(HOST, closedPort, new FrameReader(), new FrameWriter()).sendFile(input.toString()));

        assertTrue(e.getMessage().contains("Cannot connect"));
    }

    @Test
    void serverRejectsZeroLengthFrameButKeepsEarlierDataAndStaysAvailable() throws Exception {
        Result bad = sendRawAndAwait(concat(frame("PAYMENT|1|1.00|EUR"), new byte[]{0x00, 0x00}));

        assertFalse(bad.ok());
        assertEquals(1, bad.frames());
        assertTrue(bad.error().contains("Invalid frame length: 0"));
        assertEquals(List.of("PAYMENT|1|1.00|EUR"), outputLines());

        // the next client is served normally
        Result good = sendRawAndAwait(frame("PAYMENT|2|2.00|EUR"));
        assertTrue(good.ok());
        assertEquals(List.of("PAYMENT|1|1.00|EUR", "PAYMENT|2|2.00|EUR"), outputLines());
    }

    @Test
    void serverRejectsOversizedFrameLength() throws Exception {
        Result summary = sendRawAndAwait(new byte[]{0x10, 0x01}); // 4097

        assertFalse(summary.ok());
        assertTrue(summary.error().contains("4097"));
    }

    @Test
    void serverRejectsTruncatedHeaderAndTruncatedPayload() throws Exception {
        Result header = sendRawAndAwait(new byte[]{0x00});
        assertTrue(header.error().contains("incomplete length header"));

        Result payload = sendRawAndAwait(new byte[]{0x00, 0x05, 'A', 'B'});
        assertTrue(payload.error().contains("incomplete payload"));
        assertEquals(List.of(), outputLines());
    }

    @Test
    void serverRejectsPayloadContainingLineBreak() throws Exception {
        Result summary = sendRawAndAwait(new byte[]{0x00, 0x03, 'A', '\n', 'B'});

        assertFalse(summary.ok());
        assertTrue(summary.error().contains("line break"));
        assertEquals(List.of(), outputLines());
    }

    @Test
    void malformedRecordIsWrittenToFileButNotToDatabase() throws Exception {
        Result summary = sendRawAndAwait(concat(
                frame("PAYMENT|1|not-a-number|EUR"), frame("PAYMENT|2|2.00|EUR")));

        assertTrue(summary.ok());
        assertEquals(2, summary.frames());
        assertEquals(1, summary.stored());
        assertEquals(List.of("PAYMENT|1|not-a-number|EUR", "PAYMENT|2|2.00|EUR"), outputLines());
        assertEquals(List.of("PAYMENT|2|2.00|EUR"), dbRows());
    }

    @Test
    void serveLoopKeepsAcceptingClientsOneAfterAnother() throws Exception {
        executor.submit(server::serve);

        client().sendFile(inputFile(frame("PAYMENT|1|1.00|EUR")).toString());
        client().sendFile(inputFile(frame("PAYMENT|2|2.00|EUR")).toString());
        client().sendFile(inputFile(frame("PAYMENT|3|3.00|EUR")).toString());

        awaitLines(3);
        assertEquals(List.of("PAYMENT|1|1.00|EUR", "PAYMENT|2|2.00|EUR", "PAYMENT|3|3.00|EUR"), outputLines());
    }

    @Test
    void existingOutputFileIsAppendedToNotOverwritten() throws Exception {
        Path existing = dir.resolve("existing.txt");
        Files.write(existing, "OLD LINE\n".getBytes(StandardCharsets.UTF_8));
        MessageServer second = new MessageServer(0, existing.toString(), new FrameReader(), databaseManager);
        second.bind();
        try {
            Future<Result> result = executor.submit(second::serveOne);
            new MessageClient(HOST, second.getPort(), new FrameReader(), new FrameWriter())
                    .sendFile(inputFile(frame("PAYMENT|1|1.00|EUR")).toString());
            await(result);

            assertEquals(List.of("OLD LINE", "PAYMENT|1|1.00|EUR"),
                    Files.readAllLines(existing, StandardCharsets.UTF_8));
        } finally {
            second.stop();
        }
    }

    @Test
    void databaseFailureAbortsThatConnectionButServerKeepsRunning() throws Exception {
        databaseManager.failOnSave(true);
        Result failed = sendRawAndAwait(concat(frame("PAYMENT|1|1.00|EUR"), frame("PAYMENT|2|2.00|EUR")));

        assertFalse(failed.ok());
        assertTrue(failed.error().contains("Database error"));
        assertEquals(1, failed.frames());
        assertEquals(List.of("PAYMENT|1|1.00|EUR"), outputLines()); // written before the insert failed
        assertEquals(List.of(), dbRows());

        databaseManager.failOnSave(false);
        Result next = sendRawAndAwait(frame("PAYMENT|3|3.00|EUR"));

        assertTrue(next.ok());
        assertEquals(List.of("PAYMENT|3|3.00|EUR"), dbRows());
    }

    @Test
    void messagesArriveInSendOrderAcrossManyFrames() throws Exception {
        List<byte[]> frames = new ArrayList<>();
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String line = "PAYMENT|" + i + "|" + i + ".00|EUR";
            expected.add(line);
            frames.add(frame(line));
        }

        Result summary = sendFileAndAwait(inputFile(concat(frames.toArray(new byte[0][]))));

        assertEquals(500, summary.frames());
        assertEquals(expected, outputLines());
        assertEquals(500, dbRows().size());
    }
}

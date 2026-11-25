package com.globalpayments.task.common.protocol;

import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.FrameWriter;
import com.globalpayments.task.common.protocol.IReader;
import com.globalpayments.task.common.protocol.IWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    private IReader reader;
    private IWriter writer;

    @BeforeEach
    void setUp() {
        reader = new FrameReader();
        writer = new FrameWriter();
    }

    @Test
    void shouldWriteAndReadSamePayload() throws IOException {
        // Arrange
        byte[] originalPayload = "HELLO WORLD".getBytes("ISO-8859-2");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Act - write frame
        writer.writeFrame(output, originalPayload);

        // Convert output to input stream
        ByteArrayInputStream input =
                new ByteArrayInputStream(output.toByteArray());

        // Act - read frame
        byte[] receivedPayload = reader.readFrame(input);

        // Assert
        assertArrayEquals(originalPayload, receivedPayload);
    }

    @Test
    void shouldWriteAndReadMultipleFrames() throws IOException {
        // Arrange
        byte[] firstPayload = "HELLO".getBytes("ISO-8859-2");
        byte[] secondPayload = "WORLD".getBytes("ISO-8859-2");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Act - write two frames
        writer.writeFrame(output, firstPayload);
        writer.writeFrame(output, secondPayload);

        ByteArrayInputStream input =
                new ByteArrayInputStream(output.toByteArray());

        // Act - read two frames
        byte[] firstReceived = reader.readFrame(input);
        byte[] secondReceived = reader.readFrame(input);
        byte[] thirdReceived = reader.readFrame(input);

        // Assert
        assertArrayEquals(firstPayload, firstReceived);
        assertArrayEquals(secondPayload, secondReceived);
        assertNull(thirdReceived);
    }

    @Test
    void shouldPreserveCroatianCharacters() throws IOException {
        // Arrange
        String originalMessage = "UPLATA|ČĆŽŠĐ|100.50|HRK";

        byte[] originalPayload =
                originalMessage.getBytes("ISO-8859-2");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Act
        writer.writeFrame(output, originalPayload);

        ByteArrayInputStream input =
                new ByteArrayInputStream(output.toByteArray());

        byte[] receivedPayload = reader.readFrame(input);

        String receivedMessage =
                new String(receivedPayload, "ISO-8859-2");

        // Assert
        assertEquals(originalMessage, receivedMessage);
    }
}

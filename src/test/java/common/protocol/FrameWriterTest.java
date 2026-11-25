package test.java.common.protocol;


import main.java.common.protocol.FrameWriter;
import main.java.common.protocol.IWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FrameWriterTest {

    private IWriter writer;

    @BeforeEach
    void setUp() {
        writer = new FrameWriter();
    }

    @Test
    void shouldWriteCorrectFrame() throws IOException {
        // Arrange
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] payload = {
                0x48, 0x45, 0x4C, 0x4C, 0x4F
        };

        byte[] expected = {
                0x00, 0x05,
                0x48, 0x45, 0x4C, 0x4C, 0x4F
        };

        // Act
        writer.writeFrame(output, payload);

        // Assert
        assertArrayEquals(expected, output.toByteArray());
    }

    @Test
    void shouldThrowExceptionForNullPayload() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class,
                () -> writer.writeFrame(output, null));
    }

    @Test
    void shouldThrowExceptionForEmptyPayload() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(IOException.class,
                () -> writer.writeFrame(output, new byte[0]));
    }

    @Test
    void shouldThrowExceptionForPayloadLargerThan4096() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] payload = new byte[4097];

        assertThrows(IOException.class,
                () -> writer.writeFrame(output, payload));
    }

    @Test
    void shouldWriteMaximumAllowedPayload() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] payload = new byte[4096];

        writer.writeFrame(output, payload);

        byte[] result = output.toByteArray();

        assertEquals(4098, result.length);
        assertEquals(0x10, result[0] & 0xFF);
        assertEquals(0x00, result[1] & 0xFF);
    }
}


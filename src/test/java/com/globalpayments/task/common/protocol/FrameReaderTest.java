package com.globalpayments.task.common.protocol;

import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.IReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FrameReaderTest {

    private IReader reader;

    @BeforeEach
    void setUp() {
        reader = new FrameReader();
    }

    // ==========================================
    // readLength()
    // ==========================================

    @Test
    void shouldReadCorrectLength() throws IOException {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[]{0x00, 0x05});

        int length = reader.readLength(input);

        assertEquals(5, length);
    }

    @Test
    void shouldReturnMinusOneWhenStreamIsEmpty() throws IOException {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[0]);

        int length = reader.readLength(input);

        assertEquals(-1, length);
    }

    @Test
    void shouldThrowExceptionWhenHeaderIsIncomplete() {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[]{0x00});

        assertThrows(IOException.class,
                () -> reader.readLength(input));
    }

    @Test
    void shouldThrowExceptionWhenLengthIsZero() {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[]{0x00, 0x00});

        assertThrows(IOException.class,
                () -> reader.readLength(input));
    }

    @Test
    void shouldThrowExceptionWhenLengthExceeds4096() {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[]{0x10, 0x01});

        assertThrows(IOException.class,
                () -> reader.readLength(input));
    }

    // ==========================================
    // readPayload()
    // ==========================================

    @Test
    void shouldReadCorrectPayload() throws IOException {
        byte[] expected = {0x48, 0x45, 0x4C, 0x4C, 0x4F};

        ByteArrayInputStream input =
                new ByteArrayInputStream(expected);

        byte[] actual = reader.readPayload(input, 5);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldThrowExceptionWhenPayloadIsIncomplete() {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[]{0x48, 0x45, 0x4C});

        assertThrows(IOException.class,
                () -> reader.readPayload(input, 5));
    }

    // ==========================================
    // readFrame()
    // ==========================================

    @Test
    void shouldReadCompleteFrame() throws IOException {
        byte[] data = {
                0x00, 0x05,
                0x48, 0x45, 0x4C, 0x4C, 0x4F
        };

        ByteArrayInputStream input = new ByteArrayInputStream(data);

        byte[] payload = reader.readFrame(input);

        assertArrayEquals(
                new byte[]{0x48, 0x45, 0x4C, 0x4C, 0x4F},
                payload
        );
    }

    @Test
    void shouldReturnNullWhenStreamIsEmpty() throws IOException {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[0]);

        assertNull(reader.readFrame(input));
    }

    @Test
    void shouldThrowExceptionWhenFrameHeaderIsIncomplete() {
        ByteArrayInputStream input =
                new ByteArrayInputStream(new byte[]{0x00});

        assertThrows(IOException.class,
                () -> reader.readFrame(input));
    }

    @Test
    void shouldThrowExceptionWhenFramePayloadIsIncomplete() {
        byte[] data = {
                0x00, 0x05,
                0x48, 0x45, 0x4C
        };

        ByteArrayInputStream input = new ByteArrayInputStream(data);

        assertThrows(IOException.class,
                () -> reader.readFrame(input));
    }

    @Test
    void shouldReadMultipleFramesSequentially() throws IOException {
        byte[] data = {
                0x00, 0x03, 0x43, 0x41, 0x54,
                0x00, 0x04, 0x44, 0x4F, 0x47, 0x53
        };

        ByteArrayInputStream input = new ByteArrayInputStream(data);

        byte[] first = reader.readFrame(input);
        byte[] second = reader.readFrame(input);
        byte[] third = reader.readFrame(input);

        assertArrayEquals(
                new byte[]{0x43, 0x41, 0x54},
                first
        );

        assertArrayEquals(
                new byte[]{0x44, 0x4F, 0x47, 0x53},
                second
        );

        assertNull(third);
    }

    @Test
    void shouldThrowExceptionForIncompleteLengthHeader() {
        // Arrange
        byte[] data = {0x00};

        ByteArrayInputStream input =
                new ByteArrayInputStream(data);

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> reader.readFrame(input)
        );
    }

    @Test
    void shouldThrowExceptionForIncompletePayload() {
        // Arrange
        byte[] data = {
                0x00, 0x05,       // length = 5
                0x41, 0x42, 0x43  // samo 3 payload bajta: ABC
        };

        ByteArrayInputStream input =
                new ByteArrayInputStream(data);

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> reader.readFrame(input)
        );
    }
}

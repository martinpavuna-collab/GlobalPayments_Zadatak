package com.globalpayments.task.common.protocol;

import com.globalpayments.task.common.protocol.FrameReader;
import com.globalpayments.task.common.protocol.IReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    // ==========================================
    // additional protocol rules
    // ==========================================

    @Test
    void lengthIsBigEndianMsbFirst() throws IOException {
        // 0x01 0x00 = 256 (a little-endian reader would see 1)
        byte[] data = new byte[2 + 256];
        data[0] = 0x01;
        data[1] = 0x00;

        assertEquals(256, reader.readFrame(new ByteArrayInputStream(data)).length);
    }

    @Test
    void shouldAcceptMinimumAndMaximumPayloadSize() throws IOException {
        assertEquals(1, reader.readFrame(
                new ByteArrayInputStream(new byte[]{0x00, 0x01, 'X'})).length);

        byte[] max = new byte[2 + 4096];
        max[0] = 0x10;
        max[1] = 0x00;
        assertEquals(4096, reader.readFrame(new ByteArrayInputStream(max)).length);
    }

    @Test
    void shouldNotTreatHighBytesAsNegativeLength() throws IOException {
        // payload bytes >= 0x80 (ISO-8859-2 letters) must pass through unchanged
        byte[] expected = {(byte) 0xC8, (byte) 0xC6, (byte) 0xAE};
        ByteArrayInputStream input = new ByteArrayInputStream(
                new byte[]{0x00, 0x03, (byte) 0xC8, (byte) 0xC6, (byte) 0xAE});

        assertArrayEquals(expected, reader.readFrame(input));
    }

    @Test
    void shouldReadPayloadDeliveredInTinyChunks() throws IOException {
        // TCP may deliver a frame in arbitrary pieces; the reader has to keep reading.
        byte[] data = {0x00, 0x05, 'H', 'E', 'L', 'L', 'O'};
        InputStream oneByteAtATime = new InputStream() {
            private int pos;

            @Override
            public int read() {
                return pos < data.length ? data[pos++] & 0xFF : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int c = read();
                if (c == -1) {
                    return -1;
                }
                b[off] = (byte) c;
                return 1;
            }
        };

        assertArrayEquals("HELLO".getBytes(), reader.readFrame(oneByteAtATime));
        assertNull(reader.readFrame(oneByteAtATime));
    }
}

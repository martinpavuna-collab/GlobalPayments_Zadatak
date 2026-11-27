package com.globalpayments.task.common.protocol;

import com.globalpayments.task.config.AppConfig;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads frames: 2-byte unsigned big-endian length (payload bytes only) + payload.
 */
public class FrameReader implements IReader {

    /**
     * Reads the 2-byte length header.
     *
     * @return the payload length, or -1 if the stream ended cleanly before a new frame
     * @throws IOException if the header is incomplete or the length is outside 1..4096
     */
    public int readLength(InputStream inputStream) throws IOException {

        // Read first byte (MSB)
        int highByte = inputStream.read();

        // EOF before a new frame - normal end of input
        if (highByte == -1) {
            return -1;
        }

        // Read second byte (LSB)
        int lowByte = inputStream.read();

        // Incomplete header
        if (lowByte == -1) {
            throw new IOException("Invalid frame: incomplete length header.");
        }

        // Convert two bytes into an integer (big-endian)
        int length = (highByte << 8) | lowByte;

        // Validate payload length
        if (length < AppConfig.MIN_PAYLOAD_SIZE || length > AppConfig.MAX_PAYLOAD_SIZE) {
            throw new IOException("Invalid frame length: " + length
                    + " (allowed " + AppConfig.MIN_PAYLOAD_SIZE + "-" + AppConfig.MAX_PAYLOAD_SIZE + ").");
        }

        return length;
    }

    /**
     * Reads exactly {@code length} payload bytes.
     *
     * @throws IOException if the stream ends before all bytes were read
     */
    public byte[] readPayload(InputStream inputStream, int length)
            throws IOException {

        byte[] payload = new byte[length];

        int totalBytesRead = 0;

        while (totalBytesRead < length) {

            int bytesRead = inputStream.read(
                    payload,
                    totalBytesRead,
                    length - totalBytesRead
            );

            if (bytesRead == -1) {
                throw new IOException("Invalid frame: incomplete payload (got "
                        + totalBytesRead + " of " + length + " bytes).");
            }

            totalBytesRead += bytesRead;
        }

        return payload;
    }

    /**
     * Reads one whole frame.
     *
     * @return the payload, or {@code null} at a clean end of the stream
     */
    public byte[] readFrame(InputStream inputStream)
            throws IOException {

        int length = readLength(inputStream);

        // Normal EOF - no more frames
        if (length == -1) {
            return null;
        }

        return readPayload(inputStream, length);
    }
}

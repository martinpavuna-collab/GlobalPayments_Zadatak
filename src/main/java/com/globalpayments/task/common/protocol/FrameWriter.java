package com.globalpayments.task.common.protocol;

import java.io.IOException;
import java.io.OutputStream;

public class FrameWriter implements IWriter {

    private static final int MAX_FRAME_SIZE = 4096;

    @Override
    public void writeFrame(OutputStream outputStream, byte[] payload)
            throws IOException {

        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null.");
        }

        int length = payload.length;

        if (length < 1 || length > MAX_FRAME_SIZE) {
            throw new IOException("Invalid frame length: " + length);
        }

        // Write length (2 bytes, big-endian)
        outputStream.write((length >> 8) & 0xFF);
        outputStream.write(length & 0xFF);

        // Write payload
        outputStream.write(payload);

        outputStream.flush();
    }
}

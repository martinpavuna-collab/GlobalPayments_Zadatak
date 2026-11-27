package com.globalpayments.task.common.protocol;

import com.globalpayments.task.config.AppConfig;

import java.io.IOException;
import java.io.OutputStream;

public class FrameWriter implements IWriter {

    @Override
    public void writeFrame(OutputStream outputStream, byte[] payload)
            throws IOException {

        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null.");
        }

        int length = payload.length;

        if (length < AppConfig.MIN_PAYLOAD_SIZE || length > AppConfig.MAX_PAYLOAD_SIZE) {
            throw new IOException("Invalid frame length: " + length
                    + " (allowed " + AppConfig.MIN_PAYLOAD_SIZE + "-" + AppConfig.MAX_PAYLOAD_SIZE + ").");
        }

        // Write length (2 bytes, big-endian)
        outputStream.write((length >> 8) & 0xFF);
        outputStream.write(length & 0xFF);

        // Write payload
        outputStream.write(payload);

        outputStream.flush();
    }
}

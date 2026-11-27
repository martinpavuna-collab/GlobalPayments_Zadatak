package com.globalpayments.task.common.protocol;

import java.io.IOException;
import java.io.InputStream;

public interface IReader {

    int readLength(InputStream inputStream) throws IOException;

    byte[] readPayload(InputStream inputStream, int length) throws IOException;

    /** @return the next payload, or {@code null} at a clean end of the stream */
    byte[] readFrame(InputStream inputStream) throws IOException;
}

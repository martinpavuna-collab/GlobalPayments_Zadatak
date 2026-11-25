package com.globalpayments.task.common.protocol;

import java.io.IOException;
import java.io.OutputStream;

public interface IWriter {

    void writeFrame(OutputStream outputStream, byte[] payload)
            throws IOException;
}

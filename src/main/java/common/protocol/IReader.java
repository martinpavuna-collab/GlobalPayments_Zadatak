package main.java.common.protocol;

import java.io.IOException;
import java.io.InputStream;

public interface IReader {

    public  int readLength(InputStream inputStream) throws IOException;

    public byte[] readPayload(InputStream inputStream, int length)  throws IOException;

    public byte[] readFrame(InputStream inputStream) throws IOException ;
}

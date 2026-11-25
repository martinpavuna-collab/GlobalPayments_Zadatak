package main.java.common.protocol;

import java.io.IOException;
import java.io.InputStream;


public class FrameReader implements IReader {

    /**
     * Method checks for lenght of the message by checking first 2 bytes
     * @param inputStream
     * @return
     * @throws IOException
     */
    public  int readLength(InputStream inputStream) throws IOException {

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
        if (length < 1 || length > 4096) {
            throw new IOException("Invalid frame length: " + length);
        }

        return length;
    }


    /**
     * Method reads payLoad with given lenght
     * @param inputStream
     * @param length
     * @return
     * @throws IOException
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
                throw new IOException("Invalid frame: incomplete payload.");
            }

            totalBytesRead += bytesRead;
        }

        return payload;
    }



    public byte[] readFrame(InputStream inputStream)
            throws IOException {

        int length = readLength(inputStream);

        // Normal EOF - no more frames
        if (length == -1) {
            return null;
        }

        // Read payload
        byte[] payload = readPayload(inputStream, length);

        return payload;
    }

}




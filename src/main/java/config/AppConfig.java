package main.java.config;

public class AppConfig {
    public static final int SERVER_PORT = 5000;
    public static final String INPUT_FILE = "input/sample-input-hr.bin";
    public static final String OUTPUT_FILE = "output/messages.txt";
    public static final String DATABASE_URL = "jdbc:sqlite:messages.db";
    public static final String INPUT_CHARSET = "ISO-8859-2";


    public static int getPort(String[] args) {
        if (args.length == 0) {
            return SERVER_PORT;
        }

        try {
            int port = Integer.parseInt(args[0]);

            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                        "Port must be between 1 and 65535."
                );
            }

            return port;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Port must be a valid number."
            );
        }
    }
}
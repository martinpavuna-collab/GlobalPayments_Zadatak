package com.globalpayments.task.config;

/**
 * Settings of the application. The only value that can be changed from the command line is the
 * TCP port (first argument of ServerMain and ClientMain); everything else is a constant here.
 */
public final class AppConfig {

    public static final String HOST = "localhost";
    public static final int SERVER_PORT = 5000;
    public static final String INPUT_FILE = "input/sample-input-hr.bin";
    public static final String OUTPUT_FILE = "output/messages.txt";
    public static final String DATABASE_FILE = "messages.db";
    public static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    public static final String INPUT_CHARSET = "ISO-8859-2";

    // Protocol rules
    public static final int MIN_PAYLOAD_SIZE = 1;
    public static final int MAX_PAYLOAD_SIZE = 4096;

    /** Max time the server waits for data from a connected client. */
    public static final int READ_TIMEOUT_MS = 30_000;

    private AppConfig() {
    }

    public static int getPort(String[] args) {
        if (args.length == 0) {
            return SERVER_PORT;
        }

        try {
            int port = Integer.parseInt(args[0].trim());

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

    /** Returns args[index], or the default if that argument was not given. */
    public static String getArg(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }

    public static String toJdbcUrl(String databaseFile) {
        return "jdbc:sqlite:" + databaseFile;
    }
}

package com.globalpayments.task.model;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/** Parses decoded message text of the form {@code type|id|amount|currency}. */
public final class TransactionParser {

    private static final char SEPARATOR = '|';
    // Unicode-aware: sample 2 uses Croatian letters in the transaction id.
    private static final Pattern ALPHANUMERIC = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern CURRENCY = Pattern.compile("[A-Za-z0-9]{3}");

    /** Control messages that terminate a batch (samples: END, KRAJ); they are not transactions. */
    private static final Set<String> END_MARKERS = Set.of("END", "KRAJ");

    private TransactionParser() {
    }

    public static boolean isEndMarker(String line) {
        return END_MARKERS.contains(line);
    }

    public static Transaction parse(String line) throws InvalidRecordException {
        String[] fields = line.split("\\" + SEPARATOR, -1);
        if (fields.length != 4) {
            throw new InvalidRecordException("expected 4 fields separated by '" + SEPARATOR
                    + "' but found " + fields.length);
        }
        if (!ALPHANUMERIC.matcher(fields[0]).matches()) {
            throw new InvalidRecordException("invalid transaction type '" + fields[0] + "'");
        }
        if (!ALPHANUMERIC.matcher(fields[1]).matches()) {
            throw new InvalidRecordException("invalid transaction id '" + fields[1] + "'");
        }
        if (!AMOUNT.matcher(fields[2]).matches()) {
            throw new InvalidRecordException("invalid amount '" + fields[2] + "'");
        }
        if (!CURRENCY.matcher(fields[3]).matches()) {
            throw new InvalidRecordException("invalid currency '" + fields[3] + "'");
        }
        return new Transaction(fields[0], fields[1], new BigDecimal(fields[2]), fields[3]);
    }
}

package com.globalpayments.task.model;

/** A message that is a valid frame but not a valid {@code type|id|amount|currency} record. */
public class InvalidRecordException extends Exception {

    public InvalidRecordException(String message) {
        super(message);
    }
}

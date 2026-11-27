package com.globalpayments.task.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionParserTest {

    @Test
    void parsesValidRecord() throws InvalidRecordException {
        Transaction t = TransactionParser.parse("PAYMENT|12345|100.50|HRK");

        assertEquals("PAYMENT", t.type());
        assertEquals("12345", t.id());
        assertEquals("100.50", t.amount().toPlainString());
        assertEquals("HRK", t.currency());
    }

    @Test
    void acceptsCroatianLettersInId() throws InvalidRecordException {
        assertEquals("ČĆŽŠĐ", TransactionParser.parse("UPLATA|ČĆŽŠĐ|100.50|HRK").id());
        assertEquals("žćčšđ", TransactionParser.parse("STORNO|žćčšđ|42.00|EUR").id());
    }

    @Test
    void preservesAmountScale() throws InvalidRecordException {
        assertEquals("42.00", TransactionParser.parse("REFUND|98765|42.00|EUR").amount().toPlainString());
        assertEquals("7", TransactionParser.parse("REFUND|98765|7|EUR").amount().toPlainString());
    }

    @Test
    void rejectsWrongNumberOfFields() {
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("END"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|2.00"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|2.00|EUR|x"));
    }

    @Test
    void rejectsEmptyFields() {
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("|1|2.00|EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A||2.00|EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1||EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|2.00|"));
    }

    @Test
    void rejectsInvalidAmount() {
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|abc|EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|-5.00|EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|1,50|EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|1.|EUR"));
    }

    @Test
    void rejectsInvalidCurrency() {
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|2.00|EU"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|2.00|EURO"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("A|1|2.00|E R"));
    }

    @Test
    void rejectsNonAlphanumericTypeOrId() {
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("PAY MENT|1|2.00|EUR"));
        assertThrows(InvalidRecordException.class, () -> TransactionParser.parse("PAYMENT|1-2|2.00|EUR"));
    }

    @Test
    void recognisesEndMarkers() {
        assertTrue(TransactionParser.isEndMarker("END"));
        assertTrue(TransactionParser.isEndMarker("KRAJ"));
        assertFalse(TransactionParser.isEndMarker("ENDX"));
        assertFalse(TransactionParser.isEndMarker("PAYMENT|1|2.00|EUR"));
    }
}

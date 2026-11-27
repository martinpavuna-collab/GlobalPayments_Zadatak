package com.globalpayments.task.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One record of a message: {@code type|id|amount|currency}.
 */
public final class Transaction {

    private final String type;
    private final String id;
    private final BigDecimal amount;
    private final String currency;

    /**
     * @param type     transaction type, alphanumeric (e.g. PAYMENT)
     * @param id       transaction id, alphanumeric
     * @param amount   decimal amount (scale preserved, e.g. 42.00)
     * @param currency 3 alphanumeric characters (ISO 4217, e.g. HRK)
     */
    public Transaction(String type, String id, BigDecimal amount, String currency) {
        this.type = type;
        this.id = id;
        this.amount = amount;
        this.currency = currency;
    }

    public String type() {
        return type;
    }

    public String id() {
        return id;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction) o;
        return type.equals(other.type) && id.equals(other.id)
                && amount.equals(other.amount) && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, amount, currency);
    }

    @Override
    public String toString() {
        return type + "|" + id + "|" + amount.toPlainString() + "|" + currency;
    }
}

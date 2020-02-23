package com.rcjava.model;

import com.alibaba.fastjson.annotation.JSONField;


/**
 *
 * @author zyf
 */
public class Transfer {
    @JSONField(ordinal = 1)
    private String from;
    @JSONField(ordinal = 2)
    private String to;
    @JSONField(ordinal = 3)
    private int amount;

    public Transfer() {
    }

    public Transfer(String from, String to, int amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", amount=" + amount +
                '}';
    }
}

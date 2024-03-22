package io.hhplus.tdd.point.dto.response;


import io.hhplus.tdd.point.model.TransactionType;

public class PointHistoryResponse {
    private long id;
    private long userId;
    private long amount;
    private TransactionType type;
    private long updateMillis;

    // Constructors, Getters, and Setters
    public PointHistoryResponse(long id, long userId, long amount, TransactionType type, long updateMillis) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.updateMillis = updateMillis;
    }

    // Getters and setters...
}
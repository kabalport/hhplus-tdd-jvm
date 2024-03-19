package io.hhplus.tdd.point.exception;

// 예외 클래스 정의
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

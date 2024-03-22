package io.hhplus.tdd.point.model;

public class PointFailedEvent {
    private final long id; // 이벤트 ID
    private final long userId; // 사용자 ID
    private final String operation; // 수행된 작업 유형 (예: "CHARGE", "USE")
    private final long amount; // 작업에 관련된 포인트 양
    private final String errorMessage; // 오류 메시지
    private final long timestamp; // 이벤트가 발생한 시간 (밀리초 단위의 타임스탬프)


    public PointFailedEvent(long id, long userId, String operation, long amount, String errorMessage, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.operation = operation;
        this.amount = amount;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }



    // Getter 메소드들
    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getOperation() {
        return operation;
    }

    public long getAmount() {
        return amount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

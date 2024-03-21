package io.hhplus.tdd.database;

import io.hhplus.tdd.point.domain.PointFailedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PointFailedEventsTable {
    private final List<PointFailedEvent> table = new ArrayList<>();
    private long cursor = 1;

    public PointFailedEvent insert(long userId, String operation, long amount, String errorMessage, long timestamp) {
        throttle(300L); // 실패 이벤트 삽입 시에도 임의의 지연을 추가
        PointFailedEvent event = new PointFailedEvent(cursor++, userId, operation, amount, errorMessage, timestamp);
        table.add(event);
        return event;
    }

    public List<PointFailedEvent> selectAllByUserId(long userId) {
        return table.stream().filter(event -> event.getUserId() == userId).toList();
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {
        }
    }
}

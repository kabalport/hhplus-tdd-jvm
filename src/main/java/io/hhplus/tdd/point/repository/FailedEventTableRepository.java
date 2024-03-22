package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointFailedEventsTable;
import io.hhplus.tdd.point.model.PointFailedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FailedEventTableRepository implements FailedEventRepository {

    private final PointFailedEventsTable failedEventsTable;

    @Autowired
    public FailedEventTableRepository(PointFailedEventsTable failedEventsTable) {
        this.failedEventsTable = failedEventsTable;
    }

    @Override
    public PointFailedEvent save(PointFailedEvent event) {
        return failedEventsTable.insert(event.getUserId(), event.getOperation(), event.getAmount(), event.getErrorMessage(), event.getTimestamp());
    }

    @Override
    public List<PointFailedEvent> findAllByUserId(long userId) {
        return failedEventsTable.selectAllByUserId(userId);
    }
}

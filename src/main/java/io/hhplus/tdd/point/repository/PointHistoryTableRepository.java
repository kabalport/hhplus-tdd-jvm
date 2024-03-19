package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PointHistoryTableRepository implements PointHistoryRepository {

    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointHistoryTableRepository(PointHistoryTable pointHistoryTable) {
        this.pointHistoryTable = pointHistoryTable;
    }

    @Override
    public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis) {
        return pointHistoryTable.insert(userId, amount, type, updateMillis);
    }

    @Override
    public List<PointHistory> selectAllByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return pointHistoryTable.insert(pointHistory.userId(), pointHistory.amount(), pointHistory.type(), pointHistory.updateMillis());
    }
}
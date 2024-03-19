package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PointHistoryTableRepository {

    private final PointHistoryTable pointHistoryTable;

    public PointHistoryTableRepository(PointHistoryTable pointHistoryTable) {
        this.pointHistoryTable = pointHistoryTable;
    }


    public List<PointHistory> findAllByUserId(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public void save(PointHistory pointHistory) {
        pointHistoryTable.insert(pointHistory.userId(),pointHistory.amount(),pointHistory.type(),pointHistory.updateMillis());
    }
    public void save(long userId, long amount, TransactionType transactionType, long updateMillis) {
        pointHistoryTable.insert(userId,amount, transactionType, updateMillis);
    }
}

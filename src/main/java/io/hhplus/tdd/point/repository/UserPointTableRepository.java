package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.UserPoint;
import org.springframework.stereotype.Repository;

@Repository
public class UserPointTableRepository {

    private final UserPointTable userPointTable;

    public UserPointTableRepository(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    public UserPoint findById(Long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint save(UserPoint userPoint) {
        return userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
    }
}

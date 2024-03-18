package io.hhplus.tdd.point;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PointService {

    private final Map<Long, UserPoint> userPoints = new HashMap<>();
    private final Map<Long, List<PointHistory>> pointHistories = new HashMap<>();
    private long historyId = 0;

    public UserPoint getUserPoint(Long userId) {
        return userPoints.getOrDefault(userId, new UserPoint(userId, 0L, System.currentTimeMillis()));
    }

    public List<PointHistory> getPointHistories(Long userId) {
        return pointHistories.getOrDefault(userId, new ArrayList<>());
    }

    public UserPoint chargePoint(Long userId, Long amount) {
        UserPoint currentPoint = userPoints.getOrDefault(userId, new UserPoint(userId, 0L, System.currentTimeMillis()));
        UserPoint updatedPoint = new UserPoint(userId, currentPoint.point() + amount, System.currentTimeMillis());

        // Update user point
        userPoints.put(userId, updatedPoint);

        // Record history
        PointHistory history = new PointHistory(++historyId, userId, TransactionType.CHARGE, amount, System.currentTimeMillis());
        pointHistories.computeIfAbsent(userId, k -> new ArrayList<>()).add(history);

        return updatedPoint;
    }

    public UserPoint usePoint(Long userId, Long amount) {
        UserPoint currentPoint = userPoints.get(userId);
        if (currentPoint == null || currentPoint.point() < amount) {
            throw new IllegalArgumentException("Insufficient points");
        }

        UserPoint updatedPoint = new UserPoint(userId, currentPoint.point() - amount, System.currentTimeMillis());

        // Update user point
        userPoints.put(userId, updatedPoint);

        // Record history
        PointHistory history = new PointHistory(++historyId, userId, TransactionType.USE, amount, System.currentTimeMillis());
        pointHistories.computeIfAbsent(userId, k -> new ArrayList<>()).add(history);

        return updatedPoint;
    }
}

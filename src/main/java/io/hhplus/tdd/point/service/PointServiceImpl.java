package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.repository.UserPointTableRepository;
import io.hhplus.tdd.point.repository.PointHistoryTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointServiceImpl implements PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Autowired
    public PointServiceImpl(UserPointRepository userPointRepository, PointHistoryRepository pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    @Override
    public UserPoint getPointById(long id) {
        // 포인트 조회 로직
        return userPointRepository.selectById(id);
    }

    @Override
    public List<PointHistory> getHistoriesByUserId(long id) {
        // 포인트 이력 조회 로직
        return pointHistoryRepository.selectAllByUserId(id);
    }

//    @Override
//    public UserPoint chargePoint(long id, long amount) {
//        // 포인트 충전 로직
//        UserPoint currentPoint = userPointRepository.selectById(id);
//        long updatedPoint = currentPoint.point() + amount;
//        UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(id, updatedPoint);
//        pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
//        return updatedUserPoint;
//    }

    @Override
    public UserPoint chargePoint(long id, long amount) {
        // Retrieve the current points from the repository
        UserPoint currentPoint = userPointRepository.selectById(id);
        if (currentPoint == null) {
            throw new IllegalStateException("UserPoint not found for id: " + id);
        }

        // Update the points and create a new UserPoint instance for saving
        long updatedPoints = currentPoint.point() + amount;
        UserPoint updatedUserPoint = new UserPoint(id, updatedPoints, System.currentTimeMillis());

        // Save the updated points
        userPointRepository.save(updatedUserPoint);

        // Create and save a new PointHistory record
        PointHistory history = new PointHistory(id,currentPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryRepository.save(history);

        // Return the updated UserPoint
        return updatedUserPoint;
    }


    @Override
    public UserPoint usePoint(long id, long amount) {
        UserPoint currentPoint = userPointRepository.selectById(id);
        if (currentPoint.point() < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        long updatedPoints = currentPoint.point() - amount;
        UserPoint updatedUserPoint = new UserPoint(id, updatedPoints, System.currentTimeMillis());
        userPointRepository.save(updatedUserPoint);

        PointHistory history = new PointHistory(id, currentPoint.id(), -amount, TransactionType.USE, System.currentTimeMillis());
        pointHistoryRepository.save(history);

        return updatedUserPoint;
    }
}
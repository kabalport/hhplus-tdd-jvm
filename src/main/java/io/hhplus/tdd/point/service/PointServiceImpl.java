package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.UserPointTableRepository;
import io.hhplus.tdd.point.repository.PointHistoryTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointServiceImpl implements PointService {

    private final UserPointTableRepository userPointRepository;
    private final PointHistoryTableRepository pointHistoryRepository;

    @Autowired
    public PointServiceImpl(UserPointTableRepository userPointRepository, PointHistoryTableRepository pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    @Override
    public UserPoint getPointById(long userId) {
        // 사용자 포인트 조회
        return userPointRepository.findById(userId);
    }

    @Override
    public List<PointHistory> getHistoriesByUserId(long userId) {
        // 사용자의 포인트 충전/사용 내역 조회
        return pointHistoryRepository.findAllByUserId(userId);
    }

    @Override
    public UserPoint chargePoint(long userId, long amount) {
        // 포인트 충전
        UserPoint currentPoint = userPointRepository.findById(userId);
        UserPoint updatedPoint = userPointRepository.save(new UserPoint(userId, currentPoint.point() + amount, System.currentTimeMillis()));
        pointHistoryRepository.save(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return updatedPoint;
    }

    @Override
    public UserPoint usePoint(long userId, long amount) {
        // 포인트 사용
        UserPoint currentPoint = userPointRepository.findById(userId);
        if (currentPoint.point() < amount) {
            throw new RuntimeException("Insufficient points for transaction.");
        }
        UserPoint updatedPoint = userPointRepository.save(new UserPoint(userId, currentPoint.point() - amount, System.currentTimeMillis()));
        pointHistoryRepository.save(userId, amount, TransactionType.USE, System.currentTimeMillis());
        return updatedPoint;
    }
}

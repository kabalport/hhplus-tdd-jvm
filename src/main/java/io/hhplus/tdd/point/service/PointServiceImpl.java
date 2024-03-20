package io.hhplus.tdd.point.service;

import io.hhplus.tdd.exception.PointException;
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
        UserPoint userPoint = userPointRepository.selectById(id);
        if (userPoint == null) {
            throw new PointException("존재하지 않는 사용자 ID입니다.");
        }
        return userPoint;
    }

    @Override
    public List<PointHistory> getHistoriesByUserId(long id) {
        // 포인트 이력 조회 로직
        return pointHistoryRepository.selectAllByUserId(id);
    }


    @Override
    public synchronized UserPoint chargePoint(long id, long amount) {

        UserPoint currentPoint = userPointRepository.selectById(id);
        if(currentPoint == null){
            throw new PointException("아이디가 없습니다.");
        }
        if (amount < 0) {
            throw new PointException("충전포인트는 음수가 될수 없습니다.");
        }
        if(currentPoint.point()>1000000){
            throw new PointException("1000000 포인트이상 넣을수 없습니다");
        }


        long updatedPoints = currentPoint.point() + amount;
        UserPoint updatedUserPoint = new UserPoint(id, updatedPoints, System.currentTimeMillis());

        userPointRepository.save(updatedUserPoint);

        PointHistory history = new PointHistory(id,currentPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryRepository.save(history);

        return updatedUserPoint;
    }


    @Override
    public UserPoint usePoint(long id, long amount) {
        UserPoint currentPoint = userPointRepository.selectById(id);
        if (currentPoint == null) {
            throw new PointException("존재하지 않는 사용자입니다.");
        }
        if (currentPoint.point() < amount) {
            throw new PointException("포인트가 부족합니다.");
        }

        long updatedPoints = currentPoint.point() - amount;
        UserPoint updatedUserPoint = new UserPoint(id, updatedPoints, System.currentTimeMillis());
        userPointRepository.save(updatedUserPoint);

        PointHistory history = new PointHistory(id, currentPoint.id(), -amount, TransactionType.USE, System.currentTimeMillis());
        pointHistoryRepository.save(history);

        return updatedUserPoint;
    }
}
package io.hhplus.tdd.point.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hhplus.tdd.exception.PointException;
import io.hhplus.tdd.point.domain.PointFailedEvent;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.FailedEventRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointServiceImpl implements PointService {
    private static final Logger logger = LoggerFactory.getLogger(PointServiceImpl.class);

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final FailedEventRepository failedEventRepository;

    @Autowired
    public PointServiceImpl(UserPointRepository userPointRepository, PointHistoryRepository pointHistoryRepository, FailedEventRepository failedEventRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
        this.failedEventRepository = failedEventRepository;
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

    //synchronized 42초 걸림

    //



    /**
     * 동시성 고민중임
     *
     사용자별 Lock 관리
     사용자 ID별로 동시성을 관리하기 위해,
     현재 충전 중인 사용자 ID를 키로 하는 ConcurrentHashMap(동시에 접근이 가능한 자료구조)에 Lock 객체를 저장하고,
     해당 사용자에 대한 작업을 수행할 때만 Lock을 거는 방법을 고려할 수 있습니다.
     이 방식은 특정 사용자에 대한 요청을 순차적으로 처리할 수 있게 하며,
     다른 사용자의 작업에는 영향을 주지 않습니다.
     - 락을 사용할때는 데드락을 방지하기 위해 주의해야한다.
     * @param id
     * @param amount
     * @return
     */
    @Override
    public synchronized UserPoint chargePoint(long id, long amount) {
        try {
            UserPoint currentPoint = userPointRepository.selectById(id);
            if (currentPoint == null) {
                throw new PointException("아이디가 없습니다.");
            }
            if (amount < 0) {
                throw new PointException("충전포인트는 음수가 될수 없습니다.");
            }
            if (currentPoint.point() > 1000000) {
                throw new PointException("1000000 포인트이상 넣을수 없습니다");
            }


            long updatedPoints = currentPoint.point() + amount;
            UserPoint updatedUserPoint = new UserPoint(id, updatedPoints, System.currentTimeMillis());

            userPointRepository.save(updatedUserPoint);

            PointHistory history = new PointHistory(id, currentPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());
            pointHistoryRepository.save(history);

            return updatedUserPoint;
        } catch (PointException ex) {
            failedEventRepository.save(new PointFailedEvent(
                    id,id, "CHARGE", amount, ex.getMessage(), System.currentTimeMillis()
            ));
            logger.error("포인트 충전실패아이디: {}. 실패포인트: {}. 에러: {}", id, amount, ex.getMessage());
            throw ex;
        }
    }


    @Override
    public synchronized UserPoint usePoint(long id, long amount) {
        try {
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
        } catch (PointException ex) {
            failedEventRepository.save(new PointFailedEvent(
                    id,id, "USE", amount, ex.getMessage(), System.currentTimeMillis()
            ));
            logger.error("Failed to use points for user {}. Amount: {}. Error: {}", id, amount, ex.getMessage());
            throw ex;
        }
    }
}
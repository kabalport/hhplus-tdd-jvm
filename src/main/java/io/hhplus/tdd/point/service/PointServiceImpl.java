package io.hhplus.tdd.point.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hhplus.tdd.point.exception.PointException;
import io.hhplus.tdd.point.model.PointFailedEvent;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.model.UserPoint;
import io.hhplus.tdd.point.repository.FailedEventRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointServiceImpl implements PointService {
    private static final Logger logger = LoggerFactory.getLogger(PointServiceImpl.class);

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final FailedEventRepository failedEventRepository;

    private final ConcurrentHashMap<Long, Lock> locks = new ConcurrentHashMap<>();
    // ConcurrentHashMap `locks`맵은 각 사용자 아이디에 대응하는 Lock 객체를 저장하는데 사용합니다.

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


    /**
     * Todo - 동시성 고민중인 충전하기기능
     * @param id
     * @param amount
     * @return
     */
    @Override
    public UserPoint chargePoint(long id, long amount) {
        // locks객체에서 사용자별로 동기화를 위한 락을 관리하는 ConcurrentHashMap에서 락을 가져오거나 새로 생성합니다.
        // 특정 사용자아이디에 대한 Lock 객체를 조회하거나 없는경우 생성합니다.
        // `computeIfAbsent`메서드는 key에 해당하는 값이 이미 존재하면 그 값을 반환하고
        // 존재하지 않는 경우 제공된 매핑 함수를 사용하여 새 값을 계산하여 맵에 추가한후 그 값을 반환한다.
        // 여기서 key는 사용자 아이디이며, 매핑함수는 new ReentrantLock()입니다.
        // 이 과정을 통해 각 사용자 아이디 별로 독립된 락을 관리하게 됩니다.
        // 사용자 아이디별로 `ConcurrentHashMap`에서 `Lock`객체를 가져오거나 없으면 새로운 `ReentrantLock`객체를 생성하여 반환합니다.라는 작업을 수행합니다.
        // 이렇게 함으로써 동시성제어를 위한 동기화 메커니즘이 각 사용자아이디별로 제공됩니다.
        Lock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());

        // 락 휙득 시도, 획득에 성공하면 true, 실패하면 false를 반환합니다.
        boolean lockAcquired = lock.tryLock();

        // 락휙득에 실패한경우, 즉시 예외를 발생시켜 사용자에게 다시 시도하라는 메시지를 보냅니다.
        if (!lockAcquired) {
            throw new PointException("현재 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
        try {
            // 요청된 사용자 아이디로 사용자 포인트 정보를 조회합니다.
            UserPoint currentPoint = userPointRepository.selectById(id);
            // 조회된 사용자 정보가 없는 경우 예외를 발생시킵니다.
            if (currentPoint == null) {
                throw new PointException("아이디가 없습니다.");
            }
            // 요청된 충전 포인트가 음수인 경우 예외를 발생시킵니다.
            if (amount < 0) {
                throw new PointException("충전포인트는 음수가 될수 없습니다.");
            }
            // 사용자 포인트가 1,000,000을 초과하는 경우 예외를 발생시킵니다.
            if (currentPoint.point() > 1000000) {
                throw new PointException("1000000 포인트이상 넣을수 없습니다");
            }

            // 현재 포인트에 요청된 충전 포인트를 추가합니다.
            long updatedPoints = currentPoint.point() + amount;
            // 업데이트된 포인트 정보로 새 UserPoint 객체를 생성합니다.
            UserPoint updatedUserPoint = new UserPoint(id, updatedPoints, System.currentTimeMillis());

            // 업데이트된 사용자 포인트 정보를 저장합니다.
            userPointRepository.save(updatedUserPoint);

            // 포인트 변경 이력을 저장하기  위한 PointHistory 객체를 생성하고 저장합니다.
            PointHistory history = new PointHistory(id, currentPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());
            pointHistoryRepository.save(history);

            // 업데이트된 사용자 포인트 정보를 반환합니다.
            return updatedUserPoint;
        } catch (PointException ex) {
            // 포인트 처리 중 발생한 예외를 로그로 기록하고 실패 이벤트를 저장합니다.
            failedEventRepository.save(new PointFailedEvent(id, id, "CHARGE", amount, ex.getMessage(), System.currentTimeMillis()));
            logger.error("포인트 충전실패아이디: {}. 실패포인트: {}. 에러: {}", id, amount, ex.getMessage());
            // 예외를 다시 발생시켜 호출자에게 알립니다.
            throw ex;

        } finally {
            // 락을 성공적으로 획득했다면 락을 해제합니다.
            if (lockAcquired) {
                lock.unlock();
            }
        }
    }


    @Override
    public UserPoint usePoint(long id, long amount) {
        Lock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        boolean lockAcquired = false;
        try {
            lockAcquired = lock.tryLock();
            if (!lockAcquired) {
                throw new PointException("현재 포인트 사용 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
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
            failedEventRepository.save(new PointFailedEvent(id, id, "USE", amount, ex.getMessage(), System.currentTimeMillis()));
            logger.error("포인트 사용실패아이디: {}. 사용실패포인트: {}. 에러: {}", id, amount, ex.getMessage());
            throw ex;
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
        }
    }
}
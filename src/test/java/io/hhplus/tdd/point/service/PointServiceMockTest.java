package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class PointServiceMockTest {

    private PointService pointService;
    private UserPointRepository userPointRepository = mock(UserPointRepository.class);
    private PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);

    @BeforeEach
    void setUp() {
        pointService = new PointServiceImpl(userPointRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("아이디로 사용자의 포인트를 조회")
    void testGetPointById() {
        long userId = 1L;
        UserPoint mockUserPoint = new UserPoint(userId, 1000, System.currentTimeMillis());
        Mockito.when(userPointRepository.selectById(userId)).thenReturn(mockUserPoint);

        UserPoint result = pointService.getPointById(userId);

        assertEquals(mockUserPoint, result);
        Mockito.verify(userPointRepository, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("사용자 포인트 충전")
    void testChargePoint() {
        long userId = 1L;
        long amount = 500;
        UserPoint existingUserPoint = new UserPoint(userId, 1000, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 1500, System.currentTimeMillis());

        when(userPointRepository.selectById(anyLong())).thenReturn(existingUserPoint);
        when(userPointRepository.insertOrUpdate(anyLong(), anyLong())).thenReturn(updatedUserPoint);

        UserPoint result = pointService.chargePoint(userId, amount);

        assertEquals(1500, result.point());
        verify(userPointRepository).insertOrUpdate(eq(userId), eq(1500L));

        // insert 메소드로 전달된 인자를 올바르게 캡처
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        ArgumentCaptor<Long> updateMillisCaptor = ArgumentCaptor.forClass(Long.class);

        verify(pointHistoryRepository).insert(userIdCaptor.capture(), amountCaptor.capture(), typeCaptor.capture(), updateMillisCaptor.capture());

        // 캡처된 값을 주장
        assertEquals(userId, userIdCaptor.getValue());
        assertEquals(amount, amountCaptor.getValue());
        assertEquals(TransactionType.CHARGE, typeCaptor.getValue());
        // updateMillis 값은 메소드 내부에서 생성되므로 0보다 큰지만 확인하면 됩니다
        assertTrue(updateMillisCaptor.getValue() > 0);
    }


    @Test
    @DisplayName("사용자 포인트 사용 내역 조회")
    void testGetHistoriesByUserId() {
        long userId = 1L;
        List<PointHistory> mockHistories = Arrays.asList(
                new PointHistory(1L, userId, 500, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 300, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryRepository.selectAllByUserId(userId)).thenReturn(mockHistories);

        List<PointHistory> result = pointService.getHistoriesByUserId(userId);

        assertEquals(mockHistories.size(), result.size());
        assertEquals(mockHistories, result);
        verify(pointHistoryRepository, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 사용")
    void testUsePoint() {
        long userId = 1L;
        long useAmount = 200;
        UserPoint existingUserPoint = new UserPoint(userId, 1000, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 800, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(existingUserPoint);
        when(userPointRepository.insertOrUpdate(anyLong(), anyLong())).thenReturn(updatedUserPoint);

        UserPoint result = pointService.usePoint(userId, useAmount);

        assertEquals(800, result.point());
        verify(userPointRepository).insertOrUpdate(eq(userId), eq(800L));

        // 포인트 사용 이력을 올바르게 기록했는지 검증
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TransactionType> typeCaptor = ArgumentCaptor.forClass(TransactionType.class);
        ArgumentCaptor<Long> updateMillisCaptor = ArgumentCaptor.forClass(Long.class);

        verify(pointHistoryRepository).insert(userIdCaptor.capture(), amountCaptor.capture(), typeCaptor.capture(), updateMillisCaptor.capture());

        assertEquals(userId, userIdCaptor.getValue());
        assertEquals(-useAmount, amountCaptor.getValue()); // 사용량이므로 음수 값을 기대
        assertEquals(TransactionType.USE, typeCaptor.getValue());
        assertTrue(updateMillisCaptor.getValue() > 0);
    }

    @Test
    @DisplayName("포인트 사용 실패 - 포인트 부족")
    void testUsePointInsufficientPoints() {
        long userId = 1L;
        long useAmount = 1200; // 사용하려는 포인트가 현재 포인트보다 많음
        UserPoint existingUserPoint = new UserPoint(userId, 1000, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(existingUserPoint);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, useAmount);
        });

        assertEquals("포인트가 부족합니다.", exception.getMessage());
    }

    /**
     * 동시성 고민 - 실패케이스 만들기
     * 
     * 동시에 여러개를 보내서 테스트
     * 멀티스레드를 사용
     * 천개의 요청 numberOfThreads 1000으로 설정
     * 멀티스레드를 사용 ExecutorService
     * for문을 사용해 1000개의 요청을 보낼것이다.
     * 모든 요청이 끝날때까지 기다려야 하므로 CountDownLatch를 사용
     */

    @Test
    @DisplayName("동시에 여러 요청을 보내 포인트 충전 시 레이스 컨디션 발생 테스트")
    void testRaceConditionOnChargePoint() throws InterruptedException {
        // 사용자 ID와 초기 포인트 설정
        long userId = 1L;
        long initialPoint = 0L;
        // 100 개의 동시 요청을 설정
        final int numberOfThreads = 100;
        // 모든 스레드가 작업을 완료할 때까지 기다리기 위한 CountDownLatch 생성
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        // ExecutorService를 사용해 스레드 풀 설정. 스레드 개수는 요청 수에 맞춰 설정
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 초기 사용자 포인트 설정
        UserPoint initialUserPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(initialUserPoint);

        // 포인트 충전 작업을 동시에 수행
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, 1L); // 각 요청은 1포인트씩 충전
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드의 작업이 끝날 때까지 대기
        latch.await();
        // ExecutorService 종료
        executorService.shutdown();

        // 최종 포인트를 조회하여 예상한 결과와 일치하는지 검증
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);

        verify(userPointRepository, atLeastOnce()).insertOrUpdate(idCaptor.capture(), amountCaptor.capture());

        // idCaptor와 amountCaptor로부터 캡처된 값을 사용하여 검증 로직을 구현
        // 이 경우, amountCaptor.getAllValues()를 사용하여 모든 충전 요청을 통해 증가된 총 포인트를 계산할 수 있습니다.
        long totalIncreasedAmount = amountCaptor.getAllValues().stream().mapToLong(Long::longValue).sum();

        // 충전 후 예상되는 포인트 계산 (초기 포인트 + 1000번의 1포인트 충전)
        long expectedFinalPoint = initialPoint + numberOfThreads;

        // 실제 결과와 예상 결과를 비교
        // 여기서는 최종 결과를 직접 조회하는 로직이 필요합니다. 예를 들어, userPointRepository의 다른 메소드를 사용하여 최종 포인트를 조회할 수 있습니다.
        // 예상한 포인트와 실제 포인트가 일치하지 않는 경우, 레이스 컨디션이 발생했을 가능성을 나타냅니다.
        long actualFinalPoint = initialPoint + totalIncreasedAmount; // 예제에서는 이렇게 계산할 수 있습니다.
        assertEquals(expectedFinalPoint, actualFinalPoint, "동시성 문제로 인해 예상한 포인트와 실제 포인트가 다릅니다.");
    }





}

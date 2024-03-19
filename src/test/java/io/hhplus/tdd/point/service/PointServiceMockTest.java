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
        // 초기 설정
        long userId = 1L;
        UserPoint initialUserPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        Mockito.when(userPointRepository.selectById(userId)).thenReturn(initialUserPoint);

        final int numberOfThreads = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 포인트 충전 작업 수행
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // insertOrUpdate 메서드 호출을 캡처하기 위한 ArgumentCaptor
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userPointRepository, atLeastOnce()).insertOrUpdate(userIdCaptor.capture(), amountCaptor.capture());

        // 모든 호출을 통해 증가된 포인트의 총합 계산
        long totalIncreasedAmount = amountCaptor.getAllValues().stream().mapToLong(Long::longValue).sum();

        System.out.println("===");
        System.out.println(totalIncreasedAmount);
        System.out.println("===");
        System.out.println(1000L);
        System.out.println("===");

        // 예상 값 검증
        assertEquals(1000L, totalIncreasedAmount, "동시에 여러 요청을 처리했을 때 총 포인트 증가량이 예상과 다릅니다.");
    }


}

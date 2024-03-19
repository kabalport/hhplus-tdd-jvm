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
    private UserPointRepository userPointRepository;
    private PointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        userPointRepository = Mockito.mock(UserPointRepository.class);
        pointHistoryRepository = Mockito.mock(PointHistoryRepository.class);
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
        // given
        long userId = 1L;
        long amount = 500;
        UserPoint existingUserPoint = new UserPoint(userId, 1000, System.currentTimeMillis());
        UserPoint expectedUpdatedUserPoint = new UserPoint(userId, 1500, System.currentTimeMillis()); // After charging

        // Setup mocks to return expected objects on method calls
        when(userPointRepository.selectById(anyLong())).thenReturn(existingUserPoint);
        when(userPointRepository.save(any(UserPoint.class))).thenReturn(expectedUpdatedUserPoint);

        // Setup ArgumentCaptors to capture the objects passed to save methods
        ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
        ArgumentCaptor<PointHistory> pointHistoryCaptor = ArgumentCaptor.forClass(PointHistory.class);

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        System.out.println(result);

        // then
        // Assert the returned UserPoint is as expected
        assertNotNull(result, "Result should not be null.");
        assertEquals(1500, result.point(), "The user's points should be correctly updated after charging.");

        // Verify save was called with the correct UserPoint object
        verify(userPointRepository).save(userPointCaptor.capture());
        UserPoint capturedUserPoint = userPointCaptor.getValue();
        assertEquals(userId, capturedUserPoint.id());
        assertEquals(1500, capturedUserPoint.point());

        // Verify save was called with the correct PointHistory object
        verify(pointHistoryRepository).save(pointHistoryCaptor.capture());
        PointHistory capturedPointHistory = pointHistoryCaptor.getValue();
        assertEquals(userId, capturedPointHistory.userId());
        assertEquals(amount, capturedPointHistory.amount());
        assertEquals(TransactionType.CHARGE, capturedPointHistory.type());
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
        // given
        long id = 1L;
        long useAmount = 200;
        UserPoint existingUserPoint = new UserPoint(id, 1000, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(id, 800, System.currentTimeMillis()); // 포인트 사용 후 예상되는 객체

        // UserPoint 객체를 조회할 때 반환할 객체 설정
        when(userPointRepository.selectById(id)).thenReturn(existingUserPoint);

        System.out.println(existingUserPoint);
        // UserPoint 객체를 저장할 때 해당 객체를 반환하도록 설정
        when(userPointRepository.save(any(UserPoint.class))).thenReturn(updatedUserPoint);

        System.out.println(updatedUserPoint);

        // when: 사용자가 포인트를 사용
        UserPoint result = pointService.usePoint(id, useAmount);
        System.out.println(result);

        // then: 결과 검증
        assertEquals(800, result.point(), "사용 후 사용자 포인트가 올바르게 감소해야 합니다.");

        // UserPoint 저장 확인
        ArgumentCaptor<UserPoint> userPointArgumentCaptor = ArgumentCaptor.forClass(UserPoint.class);
        Mockito.verify(userPointRepository).save(userPointArgumentCaptor.capture());
        UserPoint capturedUserPoint = userPointArgumentCaptor.getValue();
        assertEquals(id, capturedUserPoint.id(), "사용자 ID가 일치해야 합니다.");
        assertEquals(800, capturedUserPoint.point(), "업데이트된 포인트가 올바르게 반영되어야 합니다.");

        // PointHistory 저장 확인
        ArgumentCaptor<PointHistory> pointHistoryArgumentCaptor = ArgumentCaptor.forClass(PointHistory.class);
        Mockito.verify(pointHistoryRepository).save(pointHistoryArgumentCaptor.capture());
        PointHistory capturedPointHistory = pointHistoryArgumentCaptor.getValue();
        assertEquals(id, capturedPointHistory.userId(), "포인트 이력의 사용자 ID가 일치해야 합니다.");
        assertEquals(-useAmount, capturedPointHistory.amount(), "포인트 이력의 사용량이 올바르게 기록되어야 합니다."); // 사용량은 음수로 기록
        assertEquals(TransactionType.USE, capturedPointHistory.type(), "포인트 이력의 타입이 '사용'이어야 합니다.");
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
        when(userPointRepository.selectById(userId)).thenReturn(initialUserPoint);

        final int numberOfThreads = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 동시 다발적 포인트 충전 작업 수행
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

        // UserPoint 저장 메서드 호출 캡처를 위한 ArgumentCaptor 설정
        ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
        verify(userPointRepository, atLeastOnce()).save(userPointCaptor.capture());

        // PointHistory 저장 메서드 호출 캡처를 위한 ArgumentCaptor 설정
        ArgumentCaptor<PointHistory> pointHistoryCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository, atLeast(numberOfThreads)).save(pointHistoryCaptor.capture());

        // 모든 충전 작업을 통해 증가된 총 포인트 계산
        long totalIncreasedAmount = userPointCaptor.getAllValues().stream().mapToLong(UserPoint::point).sum() - initialUserPoint.point() * numberOfThreads;

        System.out.println("총 증가된 포인트: " + totalIncreasedAmount);
        System.out.println("예상 증가량: " + 1000L);

        // 예상한 값과 실제 증가된 총 포인트 비교 검증
        assertEquals(1000L, totalIncreasedAmount, "동시 다발적 요청 처리 시 총 포인트 증가량이 예상과 다릅니다.");
    }

}

package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Test
    @DisplayName("사용자가 포인트를 충전하면 사용자의 포인트가 증가합니다.")
    void whenChargePoint_thenUserPointIncreases() {
        long userId = 1L;
        long initialAmount = 100L;
        long chargeAmount = 50L;

        // 초기 포인트 설정
        pointService.chargePoint(userId, initialAmount);

        // 포인트 충전
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // 검증
        assertEquals(initialAmount + chargeAmount, result.point());

        // 이력 검증
        List<PointHistory> histories = pointService.getHistoriesByUserId(userId);
        assertTrue(histories.stream().anyMatch(history ->
                history.type() == TransactionType.CHARGE && history.amount() == chargeAmount));
    }

    @Test
    @DisplayName("사용자가 포인트를 사용하면 사용자의 포인트가 감소합니다.")
    void whenUsePoint_thenUserPointDecreases() {
        long userId = 2L;
        long initialAmount = 100L;
        long useAmount = 50L;

        // 초기 포인트 설정
        pointService.chargePoint(userId, initialAmount);

        // 포인트 사용
        UserPoint result = pointService.usePoint(userId, useAmount);

        // 검증
        assertEquals(initialAmount - useAmount, result.point());

        // 이력 검증
        List<PointHistory> histories = pointService.getHistoriesByUserId(userId);
        assertTrue(histories.stream().anyMatch(history ->
                history.type() == TransactionType.USE && history.amount() == -useAmount));
    }

    /**
     * TODO - 성공테스트-동시성포인트충전
     * 동시에 여러 요청을 보낼 때 사용자 포인트 증가가 정확하게 반영되는지 테스트한다.
     * given
     * 동시에 여러개를 보내서 테스트>멀티스레드를 사용
     * 스레드수는 100으로 테스트 > threadCount 100
     * 병렬수행클래스 사용 > ExecutorService
     * when
     * 반복문으로 요청 100개 보내기
     * 모든 요청이 끝날때까지 기다리기 > CountDownLatch 사용
     * then
     *모든 요청이 처리된 후의 사용자 포인트를 검증
     */
    @Test
    @DisplayName("성공테스트-동시성포인트충전-다수의_포인트충전요청시_정확한_포인트증가를_검증합니다")
    void 다수의_포인트충전요청시_정확한_포인트증가를_검증합니다() throws InterruptedException {
        final long userId = 3L; // 테스트에 사용될 사용자 ID
        final long chargeAmount = 1L; // 각 요청에 의해 충전될 포인트 양
        final int threadCount = 100; // 동시에 실행될 스레드의 수

        // 동시성 테스트를 위한 준비: 스레드 풀과 CountDownLatch 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);


        // 모든 스레드에서 포인트 충전을 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                } finally {
                    latch.countDown(); // 작업 완료 시 래치 카운트 감소
                }
            });
        }

        latch.await(); // 모든 스레드의 작업이 완료될 때까지 대기

        // 모든 요청이 처리된 후의 사용자 포인트를 검증
        UserPoint finalUserPoint = pointService.getPointById(userId);
        assertEquals(100, finalUserPoint.point(), "포인트가 정확히 증가하지 않았습니다.");

        executorService.shutdown(); // 스레드 풀 종료
    }

    @Test
    @DisplayName("성공테스트-동시성포인트사용-다수의_포인트사용요청시_정확한포인트사용을_검증합니다")
    void 다수의_포인트사용요청시_정확한포인트사용을_검증합니다() throws InterruptedException {
        final long givenId = 4L; // 테스트에 사용될 사용자 ID
        final long chargeAmount = 1000L; // 각 요청에 의해 충전될 포인트 양
        final long givenUseAmount = 1L; // 각 요청에 의해 사용될 포인트 양
        final int threadCount = 100; // 동시에 실행될 스레드의 수

        // 데이터가 없기에 충전 한번시킴
        pointService.chargePoint(givenId, chargeAmount);


        // 동시성 테스트를 위한 준비: 스레드 풀과 CountDownLatch 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);


        // 모든 스레드에서 포인트 충전을 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    // when: 사용자가 포인트를 사용
                    pointService.usePoint(givenId, givenUseAmount);
                } finally {
                    latch.countDown(); // 작업 완료 시 래치 카운트 감소
                }
            });
        }

        latch.await(); // 모든 스레드의 작업이 완료될 때까지 대기

        // 모든 요청이 처리된 후의 사용자 포인트를 검증
        UserPoint finalUserPoint = pointService.getPointById(givenId);
        assertEquals(900, finalUserPoint.point(), "포인트가 정확히 감소되지 않았습니다.");

        executorService.shutdown(); // 스레드 풀 종료
    }

    @Test
    @DisplayName("성공테스트-동시성포인트사용-다수의_포인트사용과충전요청시_정확한_포인트사용을_검증합니다")
    void 다수의_포인트사용과_충전요청시_정확한_포인트사용을_검증합니다() throws InterruptedException {
        final long userId = 5L; // 테스트에 사용될 사용자 ID
        final long initialCharge = 5000L; // 초기 충전량
        final long chargeAmount = 1L; // 충전될 포인트 양
        final long useAmount = 1L; // 사용될 포인트 양
        final int threadCount = 100; // 동시에 실행될 스레드의 수, 절반은 사용, 절반은 충전

        // 초기 포인트 충전
        pointService.chargePoint(userId, initialCharge);

        // 동시성 테스트를 위한 준비: 스레드 풀과 CountDownLatch 생성
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 절반의 스레드에서 포인트 충전을, 나머지 절반에서 포인트 사용을 요청
        for (int i = 0; i < threadCount; i++) {
            if (i % 2 == 0) { // 짝수 번째 스레드는 충전
                executorService.execute(() -> {
                    try {
                        pointService.chargePoint(userId, chargeAmount);
                    } finally {
                        latch.countDown();
                    }
                });
            } else { // 홀수 번째 스레드는 사용
                executorService.execute(() -> {
                    try {
                        pointService.usePoint(userId, useAmount);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(); // 모든 스레드의 작업이 완료될 때까지 대기

        // 모든 요청이 처리된 후의 사용자 포인트를 검증
        UserPoint finalUserPoint = pointService.getPointById(userId);
        // 기대하는 최종 포인트: 초기 충전량 + (충전량 - 사용량) * threadCount/2
        assertEquals(initialCharge + (chargeAmount - useAmount) * threadCount / 2, finalUserPoint.point());

        executorService.shutdown(); // 스레드 풀 종료
    }



}

package io.hhplus.tdd.point.service;

import io.hhplus.tdd.exception.PointException;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.model.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    /**
     * Todo - 포인트충전
     */
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

    /**
     * Todo - 포인트사용
     */
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
     * Todo - 동시성테스트-실패케이스-같은유저아이디가 다수의 포인트충전요청시 충전에 실패합니다.
     * 실패테스트(포인트충전)-같은유저아이디가_다수의_포인트충전요청시_충전에_실패합니다
     * @throws InterruptedException
     */
    @Test
    @DisplayName("실패테스트(포인트충전)-같은유저아이디가_다수의_포인트충전요청시_충전에_실패합니다")
    void 같은유저아이디가_다수의_포인트충전요청시_충전에_실패합니다() throws InterruptedException {
        final long userId = 99L; // 테스트에 사용될 사용자 ID
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
                } catch (PointException ex) {
                    assertEquals("현재 처리 중입니다. 잠시 후 다시 시도해주세요.", ex.getMessage());
                }
                finally {
                    latch.countDown(); // 작업 완료 시 래치 카운트 감소
                }
            });
        }

        latch.await(); // 모든 스레드의 작업이 완료될 때까지 대기

        // 모든 요청이 처리된 후의 사용자 포인트를 검증
        UserPoint finalUserPoint = pointService.getPointById(userId);
        assertEquals(1, finalUserPoint.point(), "포인트가 정확히 증가하지 않았습니다.");

        executorService.shutdown(); // 스레드 풀 종료
    }


    /**
     * TODO - 성공테스트-동시성포인트충전
     * 동시에 여러 요청을 보낼 때 사용자 포인트 증가가 정확하게 반영되는지 테스트한다.
     * @throws InterruptedException
     */
    @Test
    @DisplayName("성공테스트(동시성포인트충전)-서로_다른_사용자들이_포인트충전요청시_정확한_포인트증가를_검증합니다")
    void 서로_다른_사용자들이_포인트충전요청시_정확한_포인트증가를_검증합니다() throws InterruptedException {
//     given-동시에 여러개를 보내서 테스트>멀티스레드를 사용-스레드수는 100으로 테스트 > threadCount 100
//     병렬수행클래스 사용 > ExecutorService
        final long baseUserId = 100L; // 사용자 ID 기준값
        final long chargeAmount = 1L; // 각 요청에 의해 충전될 포인트 양
        final int userCount = 100; // 테스트에 사용될 사용자 수

        // 동시성 테스트를 위한 준비: 스레드 풀과 CountDownLatch 생성
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);


        // when-반복문으로 요청 100개 보내기,모든 요청이 끝날때까지 기다리기 > CountDownLatch 사용
        // 각 스레드에서 다른 사용자 ID에 대해 포인트 충전을 요청
        for (int i = 0; i < userCount; i++) {
            final long userId = baseUserId + i; // 각 사용자 ID를 증가시키며 할당
            executorService.execute(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                } finally {
                    latch.countDown(); // 작업 완료 시 래치 카운트 감소
                }
            });
        }

        latch.await(); // 모든 스레드의 작업이 완료될 때까지 대기
        // then-모든 요청이 처리된 후의 사용자 포인트를 검증
        // 모든 요청이 처리된 후 각 사용자의 포인트를 검증
        boolean allPointsCorrect = IntStream.range(0, userCount)
                .allMatch(i -> {
                    UserPoint userPoint = pointService.getPointById(baseUserId + i);
                    return userPoint != null && userPoint.point() == chargeAmount;
                });

        assertTrue(allPointsCorrect, "모든 사용자의 포인트가 정확히 증가하지 않았습니다.");

        executorService.shutdown(); // 스레드 풀 종료
    }


    @Test
    @DisplayName("성공테스트-동시성포인트사용-같은유저의_포인트사용과_충전요청시_정확한_포인트사용을_검증합니다")
    void 같은유저의_포인트사용과_충전요청시_정확한_포인트사용을_검증합니다() throws InterruptedException {
        final long userId = 532L; // 테스트에 사용될 사용자 ID
        final long initialCharge = 5000L; // 초기 충전량
        final long chargeAmount = 1L; // 각 요청에 의해 충전될 포인트 양
        final long useAmount = 1L; // 각 요청에 의해 사용될 포인트 양
        final int threadCount = 100; // 동시에 실행될 스레드의 수, 절반은 충전, 절반은 사용

        // 초기 포인트를 충전하여 테스트 시작 조건 설정
        pointService.chargePoint(userId, initialCharge);

        ExecutorService executorService = Executors.newFixedThreadPool(50); // 동시성 테스트를 위한 스레드 풀
        CountDownLatch latch = new CountDownLatch(threadCount); // 모든 스레드가 작업을 완료할 때까지 대기하기 위한 래치

        for (int i = 2; i < threadCount+2; i++) {
            if (i % 2 == 0) { // 짝수 번째 스레드는 포인트 충전 요청
                executorService.execute(() -> {
                    try {
                        pointService.chargePoint(userId, chargeAmount);
                    } catch (PointException ex) {
                        assertEquals("현재 처리 중입니다. 잠시 후 다시 시도해주세요.", ex.getMessage());
                    }
                    finally {
                        latch.countDown(); // 작업 완료 시 래치 카운트 감소
                    }
                });
            } else { // 홀수 번째 스레드는 포인트 사용 요청
                executorService.execute(() -> {
                    try {
                        pointService.usePoint(userId, useAmount);
                    } catch (PointException ex) {
                        // 예외가 발생했다면, 예상된 예외 발생으로 간주
                        assertEquals("현재 포인트 사용 처리 중입니다. 잠시 후 다시 시도해주세요.", ex.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(); // 모든 스레드의 작업 완료를 대기

        // 최종 사용자 포인트 검증
        UserPoint finalUserPoint = pointService.getPointById(userId);
        // 예상되는 최종 포인트 값 계산 및 검증
        assertEquals(5001, finalUserPoint.point());

        executorService.shutdown(); // 스레드 풀 종료
    }


}

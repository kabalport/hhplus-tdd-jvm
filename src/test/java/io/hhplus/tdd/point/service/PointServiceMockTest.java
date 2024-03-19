package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.repository.PointHistoryTableRepository;
import io.hhplus.tdd.point.repository.UserPointTableRepository;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.service.PointServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;

public class PointServiceMockTest {

    @BeforeEach
    public void setUp() {

    }

    @Test
    @DisplayName("포인트를 조회한다.")
    public void getPoint() {
        // given
        UserPointTableRepository userPointRepository = Mockito.mock(UserPointTableRepository.class);
        PointHistoryTableRepository pointHistoryRepository = Mockito.mock(PointHistoryTableRepository.class);

        PointServiceImpl pointService = new PointServiceImpl(userPointRepository, pointHistoryRepository);


        long givenUserId = 1L;
        UserPoint expectedUserPoint = new UserPoint(givenUserId, 100L, System.currentTimeMillis());
        Mockito.when(userPointRepository.findById(anyLong())).thenReturn(expectedUserPoint);

        // when
        UserPoint resultUserPoint = pointService.getPointById(givenUserId);

        // then
        assertEquals(expectedUserPoint, resultUserPoint, "The returned UserPoint should match the expected");
    }

    @Test
    @DisplayName("포인트 내역을 조회한다.")
    public void getPointHistoryList() {
        // given
        UserPointTableRepository userPointRepository = Mockito.mock(UserPointTableRepository.class);
        PointHistoryTableRepository pointHistoryRepository = Mockito.mock(PointHistoryTableRepository.class);


        long givenUserId = 1L;
        PointHistory expectHistory1 =new PointHistory(1L, givenUserId, 100L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory expectHistory2 =new PointHistory(2L, givenUserId, 50L, TransactionType.USE, System.currentTimeMillis());
        List<PointHistory> expectedHistories = List.of(
                expectHistory1,
                expectHistory2
        );

        Mockito.when(pointHistoryRepository.findAllByUserId(givenUserId)).thenReturn(expectedHistories);
        PointServiceImpl pointService = new PointServiceImpl(userPointRepository, pointHistoryRepository);

        // when
        List<PointHistory> resultHistories = pointService.getHistoriesByUserId(givenUserId);
        expectedHistories.forEach((response)->{
            System.out.println("-----");
            System.out.println(response.id()+"  " + response.amount());
        });

        resultHistories.forEach((response)->{
            System.out.println("-----");
            System.out.println(response.id()+"  " + response.amount());
        });


        // then
        assertEquals(expectedHistories, resultHistories, "The returned point histories should match the expected list.");

    }




    @Test
    @DisplayName("포인트가 잘 충전되는지 검증합니다. / 포인트충전과 포인트내역이 한번 실행")
    public void chargePointMockTest() {
        // given
        UserPointTableRepository userPointRepository = Mockito.mock(UserPointTableRepository.class);
        PointHistoryTableRepository pointHistoryRepository = Mockito.mock(PointHistoryTableRepository.class);
        PointServiceImpl pointService = new PointServiceImpl(userPointRepository, pointHistoryRepository);

        Long givenUserID = 1L;
        Long givenAmount = 50L;
        UserPoint mockUserPoint = new UserPoint(givenUserID, 0L, System.currentTimeMillis());

        // Mockito 설정
        Mockito.when(userPointRepository.findById(givenUserID)).thenReturn(mockUserPoint);
        Mockito.when(userPointRepository.save(Mockito.any(UserPoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<UserPoint> userPointArgumentCaptor = ArgumentCaptor.forClass(UserPoint.class);
        ArgumentCaptor<PointHistory> pointHistoryArgumentCaptor = ArgumentCaptor.forClass(PointHistory.class);

        // 예상되는 객체 생성
        UserPoint expectUserPoint = new UserPoint(givenUserID, givenAmount, mockUserPoint.updateMillis());
        PointHistory expectPointHistory = new PointHistory(0, givenUserID, givenAmount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        pointService.chargePoint(givenUserID, givenAmount);

        // then
        Mockito.verify(userPointRepository).save(userPointArgumentCaptor.capture());
        UserPoint capturedUserPoint = userPointArgumentCaptor.getValue();
        Assertions.assertEquals(expectUserPoint.point(), capturedUserPoint.point(), "UserPoint's point should be increased by the given amount.");

        Mockito.verify(pointHistoryRepository).save(pointHistoryArgumentCaptor.capture());
        PointHistory capturedPointHistory = pointHistoryArgumentCaptor.getValue();
        Assertions.assertEquals(expectPointHistory.userId(), capturedPointHistory.userId(), "PointHistory's userId should match.");
        Assertions.assertEquals(expectPointHistory.amount(), capturedPointHistory.amount(), "PointHistory's amount should match the given amount.");
        Assertions.assertEquals(TransactionType.CHARGE, capturedPointHistory.type(), "PointHistory's type should be CHARGE.");
    }
}

//        - PATCH  `/point/{id}/charge` : 포인트를 충전한다.
//        - PATCH `/point/{id}/use` : 포인트를 사용한다.
//        - GET `/point/{id}` : 포인트를 조회한다.
//        - GET `/point/{id}/histories` : 포인트 내역을 조회한다.
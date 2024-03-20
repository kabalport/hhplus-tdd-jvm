package io.hhplus.tdd.point.service;

import io.hhplus.tdd.exception.PointException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * TODO - PointService 테스트코드를 작성합니다.
 */
class PointServiceMockTest {

    private PointService pointService;
    private UserPointRepository userPointRepository;
    private PointHistoryRepository pointHistoryRepository;

    /**
     * TODO - 반복되는 Mock 객체 미리 세팅
     * 재사용을 위해 property 들을 초기화해서 Repository 목킹하여 Service 인자로 사용합니다
     */
    @BeforeEach
    void setUp() {
        userPointRepository = Mockito.mock(UserPointRepository.class);
        pointHistoryRepository = Mockito.mock(PointHistoryRepository.class);
        pointService = new PointServiceImpl(userPointRepository, pointHistoryRepository);
    }

    /**
     * TODO - 성공테스트-최초회원의 포인트는 0으로 설정됩니다.
     * UserPoint의 empty 메서드는 주어진 ID를 가진 빈 UserPoint 객체를 생성합니다.
     */
    @Test
    @DisplayName("성공테스트-최초회원의 포인트는 0으로 설정됩니다.-UserPoint의 empty 메서드는 주어진 ID를 가진 빈 UserPoint 객체를 생성합니다")
    void testEmptyUserPointCreation() {
        // given: 준비
        long givenId = 1L;

        // when: 실행
        UserPoint emptyUserPoint = UserPoint.empty(givenId);

        // then: 검증
        assertNotNull(emptyUserPoint, "생성된 UserPoint 객체는 null이 아니어야 합니다.");
        assertEquals(givenId, emptyUserPoint.id(), "생성된 UserPoint 객체의 ID가 주어진 ID와 일치해야 합니다.");
        assertEquals(0, emptyUserPoint.point(), "생성된 UserPoint 객체의 포인트는 0이어야 합니다.");
    }


    /**
     * TODO - 성공테스트-포인트충전
     * 사용자는 포인트를 충전할수있습니다.
     */
    @Test
    @DisplayName("성공테스트-포인트충전:사용자는 포인트를 충전할수있습니다")
    void 포인트충전() {
        // given : 준비
        // 테스트에 필요한 데이터정의
        // givenId,givenAmount(테스트 id,충전할 포인트)
        // 테스트객체정의
        // existingUserPoint,expectedUpdatedUserPoint(기존 사용자 포인트와 충전 후 예상되는 사용자 포인트 객체를 정의)
        // 테스트대상메서드 실행시 반환객체정의
        // userPointRepository.selectById 메서드 호출시 existingUserPoint 반환
        // userPointRepository의 save 메서드가 호출될 때의 expectedUpdatedUserPoint 반환
        // ArgumentCaptor를 사용하여 userPointRepository와 pointHistoryRepository에 전달된 인자를 캡처합니다.
        long givenId = 1L;
        long givenAmount = 500L;
        UserPoint existingUserPoint = new UserPoint(givenId, 1000L, System.currentTimeMillis());
        UserPoint expectedUpdatedUserPoint = new UserPoint(givenId, 1500L, System.currentTimeMillis());
        Mockito.when(userPointRepository.selectById(givenId)).thenReturn(existingUserPoint);
        Mockito.when(userPointRepository.save(any(UserPoint.class))).thenReturn(expectedUpdatedUserPoint);
        ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
        ArgumentCaptor<PointHistory> pointHistoryCaptor = ArgumentCaptor.forClass(PointHistory.class);

        // when: 실행
        // 사용자 포인트를 충전하는 메서드를 테스트합니다.
        UserPoint result = pointService.chargePoint(givenId, givenAmount);

        // then
        assertNotNull(result, "결과는 null인지 확인");
        assertEquals(1500, result.point(), "충전 후 포인트가 정확하게 업데이트되었는지 확인");

        // save 메서드에 전달된 UserPoint 객체가 예상대로 설정되었는지 검증합니다.
        // userPointRepository의 save 메서드에 전달된 UserPoint 객체 캡처
        Mockito.verify(userPointRepository, Mockito.times(1)).save(userPointCaptor.capture());
        // 캡처된 UserPoint 객체 가져오기
        UserPoint capturedUserPoint = userPointCaptor.getValue();
        // 캡처된 객체의 ID가 예상대로인지 확인
        assertEquals(givenId, capturedUserPoint.id());
        // 캡처된 객체의 포인트 양이 예상대로인지 확인
        assertEquals(1500, capturedUserPoint.point());

        // pointHistoryRepository의 save 메서드에 전달된 PointHistory 객체가 예상대로 설정되었는지 검증합니다.
        // pointHistoryRepository의 save 메서드에 전달된 PointHistory 객체 캡처
        Mockito.verify(pointHistoryRepository, Mockito.times(1)).save(pointHistoryCaptor.capture());
        // 캡처된 PointHistory 객체 가져오기
        PointHistory capturedPointHistory = pointHistoryCaptor.getValue();
        // 캡처된 객체의 사용자 ID가 예상대로인지 확인
        assertEquals(givenId, capturedPointHistory.userId());
        // 캡처된 객체의 충전량이 예상대로인지 확인
        assertEquals(givenAmount, capturedPointHistory.amount());
        // 캡처된 객체의 트랜잭션 타입이 CHARGE인지 확인
        assertEquals(TransactionType.CHARGE, capturedPointHistory.type());
    }


    @Test
    @DisplayName("실패테스트-포인트충전실패(아이디가 없습니다)")
    void 아이디문제포인트충전실패() {
        long givenId = 1L;
        long givenAmount = 500L;
        Mockito.when(userPointRepository.selectById(givenId)).thenReturn(null);

        Exception exception = assertThrows(PointException.class, () -> {
            pointService.chargePoint(givenId,givenAmount);
        });

        assertEquals("아이디가 없습니다.", exception.getMessage());
    }


    /**
     * Todo - 실패테스트-포인트충전실패(음수포인트충전시도)
     * 클라이언트의 어떠한 이유로 음수가 요청으로 들어갔다면 포인트가 음수라는것을 exception 던져주기
     */
    @Test
    @DisplayName("실패테스트-포인트충전실패(음수포인트충전시도)")
    void 음수포인트충전시도(){
        //given
        long givenUserId = 1L;
        long givenNegativeAmount = -500L;
        UserPoint existingUserPoint = new UserPoint(givenUserId, 1000, System.currentTimeMillis());
        Mockito.when(userPointRepository.selectById(givenUserId)).thenReturn(existingUserPoint);

        // when: 실행
        Exception exception = assertThrows(PointException.class, () -> {
            pointService.chargePoint(givenUserId,givenNegativeAmount);
        });
        // then: 예외 발생을 검증합니다.
        // 포인트 사용 시 포인트가 부족하여 발생하는 예외의 메시지가 "포인트가 부족합니다."인지 검증합니다.
        assertEquals("충전포인트는 음수가 될수 없습니다.", exception.getMessage());
    }





    /**
     * TODO - 성공테스트-포인트조회
     * 사용자는 포인트를 조회할수있습니다.
     */
    @Test
    @DisplayName("성공테스트-포인트조회:사용자는 포인트를 조회할수있습니다")
    void testGetPointById() {
        // given : 준비
        // 테스트에 필요한 데이터정의 : givenId
        // 테스트객체정의 : mockUserPoint
        // 테스트대상메서드 실행시 반환객체정의: userPointRepository.selectById 메서드 호출시 mockUserPoint 반환
        long givenId = 1L;
        UserPoint mockUserPoint = new UserPoint(givenId, 1000, System.currentTimeMillis());
        Mockito.when(userPointRepository.selectById(givenId)).thenReturn(mockUserPoint);
        // when : 실행
        UserPoint result = pointService.getPointById(givenId);
        // then : 검증
        // 기대결과 검증 : when 에서의 결과로 받은 result 객체가 예상객체(mockUserPoint)랑 일치하는지 검증하기
        // userPointRepository의 selectById 메서드가 정확히 한 번 호출되었는지 검증합니다.
        assertEquals(mockUserPoint, result);
        Mockito.verify(userPointRepository, times(1)).selectById(givenId);
    }

    @Test
    @DisplayName("실패테스트-포인트조회실패(존재하지않는 아이디로 포인트 조회)")
    void testGetPointByInvalidId() {
        long invalidUserId = 999L;
        Mockito.when(userPointRepository.selectById(invalidUserId)).thenReturn(null);

        Exception exception = assertThrows(PointException.class, () -> {
            pointService.getPointById(invalidUserId);
        });

        assertEquals("존재하지 않는 사용자 ID입니다.", exception.getMessage());
    }



    /**
     * TODO - 성공테스트-포인트사용
     * 사용자는 포인트를 사용할수있습니다
     */
    @Test
    @DisplayName("성공테스트-포인트사용:사용자는 포인트를 사용할수있습니다")
    void testUsePoint() {
        // given : 준비
        long givenId = 1L;
        long givenUseAmount = 200;
        UserPoint existingUserPoint = new UserPoint(givenId, 1000, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(givenId, 800, System.currentTimeMillis());
        // UserPoint 객체를 조회할 때 반환할 객체 설정
        Mockito.when(userPointRepository.selectById(givenId)).thenReturn(existingUserPoint);
        // UserPoint 객체를 저장할 때 해당 객체를 반환하도록 설정
        Mockito.when(userPointRepository.save(any(UserPoint.class))).thenReturn(updatedUserPoint);

        // when: 사용자가 포인트를 사용
        UserPoint result = pointService.usePoint(givenId, givenUseAmount);

        // then: 결과 검증
        assertEquals(800, result.point(), "사용 후 사용자 포인트가 올바르게 감소해야 합니다.");
        // UserPoint 저장 확인
        ArgumentCaptor<UserPoint> userPointArgumentCaptor = ArgumentCaptor.forClass(UserPoint.class);
        Mockito.verify(userPointRepository).save(userPointArgumentCaptor.capture());
        UserPoint capturedUserPoint = userPointArgumentCaptor.getValue();
        assertEquals(givenId, capturedUserPoint.id(), "사용자 ID가 일치해야 합니다.");
        assertEquals(800, capturedUserPoint.point(), "업데이트된 포인트가 올바르게 반영되어야 합니다.");
        // PointHistory 저장 확인
        ArgumentCaptor<PointHistory> pointHistoryArgumentCaptor = ArgumentCaptor.forClass(PointHistory.class);
        Mockito.verify(pointHistoryRepository).save(pointHistoryArgumentCaptor.capture());
        PointHistory capturedPointHistory = pointHistoryArgumentCaptor.getValue();
        assertEquals(givenId, capturedPointHistory.userId(), "포인트 이력의 사용자 ID가 일치해야 합니다.");
        assertEquals(-givenUseAmount, capturedPointHistory.amount(), "포인트 이력의 사용량이 올바르게 기록되어야 합니다."); // 사용량은 음수로 기록
        assertEquals(TransactionType.USE, capturedPointHistory.type(), "포인트 이력의 타입이 '사용'이어야 합니다.");
    }

    @Test
    @DisplayName("실패테스트-포인트사용실패(존재하지않는아이디로포인트사용)")
    void 존재하지않는아이디로포인트사용() {
        long invalidUserId = 999L;
        long amount = 500;
        when(userPointRepository.selectById(invalidUserId)).thenReturn(null); // 혹은 적절한 예외 처리

        Exception exception = assertThrows(PointException.class, () -> {
            pointService.usePoint(invalidUserId, amount);
        });

        assertEquals("존재하지 않는 사용자입니다.", exception.getMessage());
    }

    /**
     * TODO - 실패테스트-포인트사용실패(포인트부족)
     * 사용자는 포인트가 부족하여 사용에 실패합니다
     */
    @Test
    @DisplayName("실패테스트-포인트사용실패(포인트부족)-사용자는 포인트가 부족하여 사용에 실패합니다")
    void testUsePointInsufficientPoints() {
        // given: 준비
        // 테스트대상: givenUserId,givenUseAmount(사용자아이디,사용포인트양)
        // mock 객체정의 : existingUserPoint(포인트가 사용포인트량보다 작게 정의)
        // 실행반환객체정의 : userPointRepository의 selectById 메서드가 호출될 때, existingUserPoint 객체를 반환하도록 설정합니다.
        long givenUserId = 1L;
        long givenUseAmount = 1200;
        // 사용하려는 포인트가 현재 포인트보다 많음
        UserPoint existingUserPoint = new UserPoint(givenUserId, 1000, System.currentTimeMillis());
        Mockito.when(userPointRepository.selectById(givenUserId)).thenReturn(existingUserPoint);
        // when: 실행
        Exception exception = assertThrows(PointException.class, () -> {
            pointService.usePoint(givenUserId, givenUseAmount);
        });
        // then: 예외 발생을 검증합니다.
        // 포인트 사용 시 포인트가 부족하여 발생하는 예외의 메시지가 "포인트가 부족합니다."인지 검증합니다.
        assertEquals("포인트가 부족합니다.", exception.getMessage());
    }

    /**
     * TODO - 성공테스트-포인트사용내역조회
     * 사용자는 포인트사용내역을 조회할수있습니다
     */
    @Test
    @DisplayName("성공테스트-포인트사용내역조회-사용자는 포인트사용내역을 조회할수있습니다")
    void testGetHistoriesByUserId() {
        // given : 준비
        // 테스트에 필요한 데이터정의 : givenUserId
        // 테스트객체정의 : mockHistories
        // 테스트대상메서드 실행시 반환객체정의: PointHistoryRepository.selectAllByUserId 메서드 호출시 mockHistories 반환
        long givenUserId = 1L;
        List<PointHistory> mockHistories = Arrays.asList(
                new PointHistory(1L, givenUserId, 500, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, givenUserId, 300, TransactionType.USE, System.currentTimeMillis())
        );
        Mockito.when(pointHistoryRepository.selectAllByUserId(givenUserId)).thenReturn(mockHistories);
        // when : 실행
        // 포인트 서비스의 getHistoriesByUserId 메서드를 호출하여, 주어진 사용자 ID에 대한 포인트 사용 내역을 조회합니다.
        List<PointHistory> result = pointService.getHistoriesByUserId(givenUserId);

        // then: 검증
        assertEquals(mockHistories.size(), result.size(), "조회된 포인트사용내역 크기가 예상과 일치하는지 확인");
        assertEquals(mockHistories, result, "조회된 포인트사용내역이 예상과 일치하는지 확인");
        Mockito.verify(pointHistoryRepository, times(1)).selectAllByUserId(givenUserId);
    }
}

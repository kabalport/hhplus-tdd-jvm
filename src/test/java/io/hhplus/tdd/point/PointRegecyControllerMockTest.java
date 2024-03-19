package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@WebMvcTest(PointRegecyController.class)
public class PointRegecyControllerMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPointTable userPointTable;

    @MockBean
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("first mock test - 포인트가 잘 충전되는지 확인하는 테스트")
    public void firstMockTest() throws Exception{

    }

    @Test
    @DisplayName("유저 포인트 조회 테스트")
    public void testGetUserPoint() throws Exception {
        long userId = 1L;
        UserPoint mockUserPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        given(userPointTable.selectById(userId)).willReturn(mockUserPoint);

        mockMvc.perform(get("/point/" + userId))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockUserPoint)));
    }

    @Test
    @DisplayName("유저 포인트 충전 테스트")
    public void testChargeUserPoint() throws Exception {
        long userId = 1L;
        long chargeAmount = 100L;
        UserPoint mockUserPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
        given(userPointTable.selectById(any(Long.class))).willReturn(UserPoint.empty(userId));
        given(userPointTable.insertOrUpdate(any(Long.class), any(Long.class))).willReturn(mockUserPoint);

        mockMvc.perform(patch("/point/" + userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockUserPoint)));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(userPointTable).insertOrUpdate(idCaptor.capture(), amountCaptor.capture());
        Assertions.assertEquals(userId, idCaptor.getValue());
        Assertions.assertEquals(chargeAmount, amountCaptor.getValue());
    }

    @Test
    @DisplayName("유저 포인트 사용 테스트")
    public void testUseUserPoint() throws Exception {
        long userId = 1L;
        long useAmount = 50L;
        UserPoint mockUserPoint = new UserPoint(userId, 50L, System.currentTimeMillis());
        given(userPointTable.selectById(any(Long.class))).willReturn(new UserPoint(userId, 100L, System.currentTimeMillis()));
        given(userPointTable.insertOrUpdate(any(Long.class), any(Long.class))).willReturn(mockUserPoint);

        mockMvc.perform(patch("/point/" + userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockUserPoint)));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(userPointTable).insertOrUpdate(idCaptor.capture(), amountCaptor.capture());
        Assertions.assertEquals(userId, idCaptor.getValue());
        Assertions.assertTrue(amountCaptor.getValue() < 100L); // 사용 후 포인트가 줄어들어야 함
    }

    // 추가 테스트 케이스는 비슷한 패턴으로 작성할 수 있습니다.

    @Test
    @DisplayName("포인트 충전/사용 내역 조회 테스트")
    public void testGetPointHistory() throws Exception {
        long userId = 1L;
        List<PointHistory> mockHistoryList = Arrays.asList(
                new PointHistory(1L, userId, 100L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 50L, TransactionType.USE, System.currentTimeMillis())
        );

        given(pointHistoryTable.selectAllByUserId(userId)).willReturn(mockHistoryList);

        mockMvc.perform(get("/point/" + userId + "/histories"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockHistoryList)));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(pointHistoryTable).selectAllByUserId(idCaptor.capture());
        Assertions.assertEquals(userId, idCaptor.getValue());
    }

    @Test
    @DisplayName("잔액 부족으로 포인트 사용 실패 테스트")
    public void testUsePointWithInsufficientFunds() throws Exception {
        long userId = 1L;
        long useAmount = 150L; // 잔액보다 많은 양을 사용하려는 시도
        UserPoint mockUserPoint = new UserPoint(userId, 100L, System.currentTimeMillis()); // 현재 잔액

        given(userPointTable.selectById(userId)).willReturn(mockUserPoint);

        mockMvc.perform(patch("/point/" + userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isBadRequest()); // 잔액 부족으로 인한 요청 실패

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(userPointTable, Mockito.times(1)).selectById(idCaptor.capture());
        Mockito.verify(userPointTable, Mockito.never()).insertOrUpdate(idCaptor.capture(), amountCaptor.capture());
        Assertions.assertEquals(userId, idCaptor.getValue());
    }
}

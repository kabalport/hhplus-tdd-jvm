package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @BeforeEach
    public void setup() {
        // Setup mock before each test if needed
    }

    @Test
    public void testChargePoint() throws Exception {
        Long userId = 1L;
        Long chargeAmount = 100L;
        UserPoint expectedUserPoint = new UserPoint(userId, 100L, System.currentTimeMillis());

        given(pointService.chargePoint(userId, chargeAmount)).willReturn(expectedUserPoint);

        mockMvc.perform(patch("/point/" + userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point", is(expectedUserPoint.point().intValue())));
    }

    @Test
    public void testUsePoint() throws Exception {
        Long userId = 1L;
        Long useAmount = 50L;
        UserPoint expectedUserPoint = new UserPoint(userId, 50L, System.currentTimeMillis());

        given(pointService.usePoint(userId, useAmount)).willReturn(expectedUserPoint);

        mockMvc.perform(patch("/point/" + userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point", is(expectedUserPoint.point().intValue())));
    }

    @Test
    public void testGetUserPoint() throws Exception {
        Long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 100L, System.currentTimeMillis());

        given(pointService.getUserPoint(userId)).willReturn(userPoint);

        mockMvc.perform(get("/point/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point", is(userPoint.point().intValue())));
    }

    @Test
    public void testGetPointHistories() throws Exception {
        Long userId = 1L;
        PointHistory history = new PointHistory(1L, userId, TransactionType.CHARGE, 100L, System.currentTimeMillis());

        given(pointService.getPointHistories(userId)).willReturn(Arrays.asList(history));

        mockMvc.perform(get("/point/" + userId + "/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount", is(history.amount().intValue())));
    }

    @Test
    public void testUsePointInsufficientBalance() throws Exception {
        Long userId = 1L;
        Long useAmount = 150L; // Assume the user does not have enough points

        given(pointService.usePoint(userId, useAmount)).willThrow(new IllegalArgumentException("Insufficient points"));

        mockMvc.perform(patch("/point/" + userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Insufficient points")));
    }

    // You might need to implement additional tests to simulate and verify the sequential processing of multiple requests.
}

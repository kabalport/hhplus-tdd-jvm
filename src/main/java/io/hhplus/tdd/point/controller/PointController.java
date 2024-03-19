package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.dto.request.ChargePointRequest;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.UserPointResponse;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.domain.UserPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    @Autowired
    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPointResponse point(
            @PathVariable long id
    ) {
        UserPoint userPoint = pointService.getPointById(id);

        return new UserPointResponse(userPoint.id(), userPoint.point());
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistoryResponse> history(
            @PathVariable long id
    ) {
        List<PointHistory> historyList = pointService.getHistoriesByUserId(id);
        return historyList.stream()
                .map(ph -> new PointHistoryResponse(ph.id(), ph.userId(), ph.amount(), ph.type(), ph.updateMillis()))
                .collect(Collectors.toList());
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPointResponse charge(
            @PathVariable long id,
            @RequestBody ChargePointRequest request
    ) {
        UserPoint updatedUserPoint = pointService.chargePoint(id, request.getAmount());
        log.info("Charging {} points for user {}", request.getAmount(), id);

        return new UserPointResponse(updatedUserPoint.id(), updatedUserPoint.point());
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPointResponse use(
            @PathVariable long id, @RequestBody ChargePointRequest request) {
        UserPoint updatedUserPoint = pointService.usePoint(id, request.getAmount());
        return new UserPointResponse(updatedUserPoint.id(), updatedUserPoint.point());
    }
}

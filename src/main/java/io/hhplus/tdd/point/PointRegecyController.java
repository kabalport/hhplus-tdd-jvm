package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/point")
@RestController
public class PointRegecyController {


    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;


    public PointRegecyController(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(@PathVariable Long id) {
        return userPointTable.selectById(id);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(@PathVariable Long id) {

        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public synchronized UserPoint charge(@PathVariable Long id, @RequestBody Long amount) {


            // Update the user's point
            UserPoint userPoint = userPointTable.selectById(id);
            userPoint = userPointTable.insertOrUpdate(id, userPoint.point() + amount);
            // Record this transaction
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return userPoint;

    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public synchronized UserPoint use(@PathVariable Long id, @RequestBody Long amount) {


            // Retrieve the current points
            UserPoint userPoint = userPointTable.selectById(id);

            if (userPoint.point() < amount) {
                throw new RuntimeException("Insufficient points");
            }


            // Update the user's points
            userPoint = userPointTable.insertOrUpdate(id, userPoint.point() - amount);

            // Record this transaction
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
            return userPoint;
        }

}

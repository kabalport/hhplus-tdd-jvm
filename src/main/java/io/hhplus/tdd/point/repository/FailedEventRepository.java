package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.model.PointFailedEvent;
import java.util.List;

public interface FailedEventRepository {
    PointFailedEvent save(PointFailedEvent event); // 실패 이벤트 저장
    List<PointFailedEvent> findAllByUserId(long userId); // 특정 사용자 ID에 대한 모든 실패 이벤트 조회
}

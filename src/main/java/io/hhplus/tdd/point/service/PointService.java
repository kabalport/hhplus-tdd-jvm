package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;

import java.util.List;

public interface PointService {
    UserPoint getPointById(long userId);
    List<PointHistory> getHistoriesByUserId(long userId);
    UserPoint chargePoint(long userId, long amount);
    UserPoint usePoint(long id, long amount);
}
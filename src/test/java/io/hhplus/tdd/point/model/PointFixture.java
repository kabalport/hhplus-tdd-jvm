package io.hhplus.tdd.point.model;

import io.hhplus.tdd.point.model.UserPoint;

public class PointFixture {
    public static UserPoint firstUser(){
        return UserPoint
                .builder()
                .id(99900L)
                .point(0L)
                .updateMillis(System.currentTimeMillis()).build();
    }
    public static UserPoint passed(){
        return UserPoint
                .builder()
                .id(99900L)
                .point(1000L)
                .updateMillis(System.currentTimeMillis()).build();
    }

    public static UserPoint failed(){
        return UserPoint
                .builder()
                .id(999990L)
                .point(-1000L)
                .updateMillis(System.currentTimeMillis()).build();
    }
}

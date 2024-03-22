package io.hhplus.tdd.point.model;

import lombok.Builder;
@Builder
public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
//    @Builder
//    public static UserPoint createUserPoint(long id, long point, long updateMillis) {
//        return new UserPoint(id, point, updateMillis);
//    }
}

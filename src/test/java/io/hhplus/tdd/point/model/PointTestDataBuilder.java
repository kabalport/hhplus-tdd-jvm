package io.hhplus.tdd.point.model;

public class PointTestDataBuilder {

    public static UserPoint ExistPointUser(long chargeAmount) {
        long id = 999999L;
        long initialPoints = 0;
        long updateMillis = System.currentTimeMillis();

        return UserPoint.createUserPoint(id, initialPoints + chargeAmount, updateMillis);
    }

    // Success scenario: valid charge amount
    public static UserPoint ChargePointUser(long id, long chargeAmount) {
        // Assuming the initial point is 0 for simplicity
        long initialPoints = 0;
        long updateMillis = System.currentTimeMillis();

        return UserPoint.createUserPoint(id, initialPoints + chargeAmount, updateMillis);
    }

    // Failure scenario: invalid charge amount (less than or equal to 0)
    public static UserPoint ChargePointUser2(long id, long chargeAmount) {
        // This method simply prepares a user point with an attempt to charge an invalid amount
        // The actual "failure" logic would depend on how the service handles this case,
        // typically by not allowing the operation and possibly throwing an exception.
        long initialPoints = 1000; // Starting with some points for contrast
        long updateMillis = System.currentTimeMillis();

        return UserPoint.createUserPoint(id, initialPoints + chargeAmount, updateMillis);
    }
}

package io.hhplus.tdd.point.dto.response;


public class UserPointResponse {
    private long id;
    private long point;

    // Constructors, getters, and setters
    public UserPointResponse(long id, long point) {
        this.id = id;
        this.point = point;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPoint() {
        return point;
    }

    public void setPoint(long point) {
        this.point = point;
    }
}

package io.github.gaming32.mcab;

public class RemotePlayer {
    public final int enitityId;
    public double x, y;
    public float yaw;

    public RemotePlayer(int entityId) {
        this.enitityId = entityId;
    }

    public RemotePlayer(int entityId, double x, double y) {
        this(entityId);
        this.x = x;
        this.y = y;
    }

    public RemotePlayer(int entityId, double x, double y, float yaw) {
        this(entityId, x, y);
        this.yaw = yaw;
    }
}

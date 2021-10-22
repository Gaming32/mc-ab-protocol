package io.github.gaming32.mcab;

public class Vector2Int {
    public final long x, y;

    public Vector2Int(long x, long y) {
        this.x = x;
        this.y = y;
    }

    public Vector2Int() {
        this(0, 0);
    }

    public Vector2Int(Vector2Int other) {
        this(other.x, other.y);
    }

    @Override
    public String toString() {
        return String.format("Vector2Int{%d, %d}", x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Vector2Int) {
            Vector2Int other = (Vector2Int)o;
            return this.x == other.x && this.y == other.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)(x ^ y);
    }
}

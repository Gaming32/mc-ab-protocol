package io.github.gaming32.mcab.and_beyond;

public class WorldChunk {
    public final int x, y;
    public final long absX, absY;
    protected final byte[] data;
    private int version = -1;

    public WorldChunk(int x, int y, long absX, long absY, byte[] data) {
        this.x = x;
        this.y = y;
        this.absX = absX;
        this.absY = absY;
        this.data = data;
    }

    protected int getTileAddress(int x, int y) {
        return (x * 16 + y) * 2;
    }

    public BlockType getTileType(int x, int y) {
        int addr = getTileAddress(x, y);
        return BlockType.values()[data[addr]];
    }

    public void setTileType(int x, int y, BlockType type) {
        int addr = getTileAddress(x, y);
        data[addr] = (byte)type.ordinal();
    }

    public byte[] getData() {
        return data;
    }

    public int getVersion() {
        if (version < 0) {
            byte a = data[512],
                 b = data[513],
                 c = data[514],
                 d = data[515];
            version = a + (b << 8) + (c << 16) + (d << 24);
        }
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
        int v = version >> 24;
        data[515] = (byte)v;
        version -= v << 24;
        v = version >> 16;
        data[514] = (byte)v;
        version -= v << 16;
        v = version >> 8;
        data[513] = (byte)v;
        version -= v << 8;
        data[512] = (byte)version;
    }

    public boolean hasGenerated() {
        return getVersion() > 0;
    }

    public static enum BlockType {
        AIR(0, 0), // 0
        STONE(1, 1), // 1
        DIRT(10, 15), // 2
        GRASS(9, 14), // 3
        WOOD(77, 101), // 4
        PLANKS(15, 22), // 5
        LEAVES(155, 133); // 6

        public final int minecraftID;
        public final int minecraftItemID;

        BlockType(int minecraftID, int minecraftItemID) {
            this.minecraftID = minecraftID;
            this.minecraftItemID = minecraftItemID;
        }
    }
}

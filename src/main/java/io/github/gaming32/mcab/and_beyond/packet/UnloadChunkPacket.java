package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UnloadChunkPacket extends Packet {
    public long x, y;

    public UnloadChunkPacket() {
        TYPE = PacketType.CHUNK_UNLOAD;
    }

    public UnloadChunkPacket(long x, long y) {
        this();
        this.x = x;
        this.y = y;
    }

    @Override
    public void read(InputStream input) throws IOException {
        x = readVarint(input);
        y = readVarint(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeVarint(x, output);
        writeVarint(y, output);
    }
}

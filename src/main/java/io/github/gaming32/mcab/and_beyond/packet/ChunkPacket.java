package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.gaming32.mcab.and_beyond.WorldChunk;

public class ChunkPacket extends Packet {
    public WorldChunk chunk;

    public ChunkPacket() {
        TYPE = PacketType.CHUNK;
    }

    public ChunkPacket(WorldChunk chunk) {
        this();
        this.chunk = chunk;
    }

    @Override
    public void read(InputStream input) throws IOException {
        long absX = Packet.readVarint(input);
        long absY = Packet.readVarint(input);
        int x = (int)Packet.readVarint(input);
        int y = (int)Packet.readVarint(input);
        byte[] data = new byte[1024];
        input.read(data);
        chunk = new WorldChunk(x, y, absX, absY, data);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        if (chunk == null) {
            output.write(new byte[1024]);
            return;
        }
        writeVarint(chunk.absX, output);
        writeVarint(chunk.absY, output);
        writeVarint(chunk.x, output);
        writeVarint(chunk.y, output);
        output.write(chunk.getData());
    }
}

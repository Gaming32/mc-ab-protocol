package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.gaming32.mcab.and_beyond.WorldChunk.BlockType;

public class ChunkUpdatePacket extends Packet {
    public long cx, cy;
    public int bx, by;
    public BlockType block;

    public ChunkUpdatePacket() {
        TYPE = PacketType.CHUNK_UPDATE;
    }

    public ChunkUpdatePacket(long cx, long cy, int bx, int by, BlockType block) {
        this();
        this.cx = cx;
        this.cy = cy;
        this.bx = bx;
        this.by = by;
    }

    @Override
    public void read(InputStream input) throws IOException {
        cx = readVarint(input);
        cy = readVarint(input);
        byte[] blockInfo = new byte[3];
        input.read(blockInfo);
        bx = blockInfo[0];
        by = blockInfo[1];
        block = BlockType.values()[blockInfo[2]];
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeVarint(cx, output);
        writeVarint(cy, output);
        output.write(new byte[] {(byte)bx, (byte)by, (byte)block.ordinal()});
    }
}

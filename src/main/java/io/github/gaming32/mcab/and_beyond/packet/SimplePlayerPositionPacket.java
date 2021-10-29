package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SimplePlayerPositionPacket extends Packet {
    public double x, y;

    public SimplePlayerPositionPacket() {
        TYPE = PacketType.SIMPLE_PLAYER_POS;
    }

    public SimplePlayerPositionPacket(double x, double y) {
        this();
        this.x = x;
        this.y = y;
    }

    @Override
    public void read(InputStream input) throws IOException {
        x = readDouble(input);
        y = readDouble(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeDouble(x, output);
        writeDouble(y, output);
    }
}

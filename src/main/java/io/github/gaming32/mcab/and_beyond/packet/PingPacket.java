package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PingPacket extends Packet {
    public PingPacket() {
        TYPE = PacketType.PING;
    }

    @Override
    public void read(InputStream input) throws IOException {
    }

    @Override
    public void write(OutputStream output) throws IOException {
    }
}

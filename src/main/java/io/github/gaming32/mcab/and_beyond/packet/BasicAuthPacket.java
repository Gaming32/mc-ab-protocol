package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BasicAuthPacket extends Packet {
    public byte[] token;

    public BasicAuthPacket() {}

    public BasicAuthPacket(byte[] token) {
        TYPE = PacketType.BASIC_AUTH;
        this.token = token;
    }

    @Override
    public void read(InputStream input) throws IOException {
        token = readBinary(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeBinary(token, output);
    }
}

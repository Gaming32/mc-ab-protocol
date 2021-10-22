package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientRequestPacket extends Packet {
    public int protocolVersion;

    public ClientRequestPacket() {
        TYPE = PacketType.CLIENT_REQUEST;
    }

    public ClientRequestPacket(int protocolVersion) {
        this();
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void read(InputStream input) throws IOException {
        protocolVersion = (int)readVarint(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeVarint(protocolVersion, output);
    }
}

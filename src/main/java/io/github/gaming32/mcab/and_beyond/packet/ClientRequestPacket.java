package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientRequestPacket extends Packet {
    public int protocolVersion;

    public ClientRequestPacket() {}

    public ClientRequestPacket(int protocolVersion) {
        TYPE = PacketType.CLIENT_REQUEST;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void read(InputStream input) throws IOException {
        protocolVersion = readVarint(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeVarint(protocolVersion, output);
    }
}

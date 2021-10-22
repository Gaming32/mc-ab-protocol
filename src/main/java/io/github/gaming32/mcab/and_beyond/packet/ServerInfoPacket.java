package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerInfoPacket extends Packet {
    public boolean offline;
    public byte[] publicKey;

    public ServerInfoPacket() {}

    public ServerInfoPacket(boolean offline, byte[] publicKey) {
        TYPE = PacketType.SERVER_INFO;
        this.offline = offline;
        this.publicKey = publicKey;
    }

    @Override
    public void read(InputStream input) throws IOException {
        offline = readBoolean(input);
        publicKey = readBinary(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeBoolean(offline, output);
        writeBinary(publicKey, output);
    }
}

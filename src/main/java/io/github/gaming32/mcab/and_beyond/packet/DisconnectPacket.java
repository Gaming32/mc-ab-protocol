package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DisconnectPacket extends Packet {
    public String reason;

    public DisconnectPacket() {
        TYPE = PacketType.DISCONNECT;
    }

    public DisconnectPacket(String reason) {
        this();
        this.reason = reason;
    }

    @Override
    public void read(InputStream input) throws IOException {
        reason = readString(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeString(reason, output);
    }
}

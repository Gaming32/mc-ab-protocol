package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ChatPacket extends Packet {
    public String message;
    public double time;

    public ChatPacket() {
        TYPE = PacketType.CHAT;
    }

    public ChatPacket(String message, double time) {
        this();
        this.message = message;
        this.time = time;
    }

    @Override
    public void read(InputStream input) throws IOException {
        message = readString(input);
        time = readDouble(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeString(message, output);
        writeDouble(time, output);
    }
}

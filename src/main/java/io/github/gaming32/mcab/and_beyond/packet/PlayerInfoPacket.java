package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PlayerInfoPacket extends Packet {
    public UUID uuid;
    public String name;

    public PlayerInfoPacket() {
        TYPE = PacketType.PLAYER_INFO;
    }

    public PlayerInfoPacket(UUID uuid, String name) {
        this();
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public void read(InputStream input) throws IOException {
        uuid = readUUID(input);
        name = new String(readBinary(input), StandardCharsets.US_ASCII);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeUUID(uuid, output);
        writeBinary(name.getBytes(StandardCharsets.US_ASCII), output);
    }
}

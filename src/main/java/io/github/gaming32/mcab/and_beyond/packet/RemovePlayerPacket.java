package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class RemovePlayerPacket extends Packet {
    public UUID player;

    public RemovePlayerPacket() {
        TYPE = PacketType.REMOVE_PLAYER;
    }

    public RemovePlayerPacket(UUID player) {
        this();
        this.player = player;
    }

    @Override
    public void read(InputStream input) throws IOException {
        player = readUUID(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeUUID(player, output);
    }
}

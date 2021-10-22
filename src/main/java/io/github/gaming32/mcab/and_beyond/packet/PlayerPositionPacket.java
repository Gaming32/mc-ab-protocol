package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PlayerPositionPacket extends Packet {
    public UUID player;
    public double x, y;

    public PlayerPositionPacket() {
        TYPE = PacketType.PLAYER_POS;
    }

    public PlayerPositionPacket(UUID player, double x, double y) {
        this();
        this.player = player;
        this.x = x;
        this.y = y;
    }

    @Override
    public void read(InputStream input) throws IOException {
        player = readUUID(input);
        x = readDouble(input);
        y = readDouble(input);
    }

    @Override
    public void write(OutputStream output) throws IOException {
        writeUUID(player, output);
        writeDouble(x, output);
        writeDouble(y, output);
    }
}

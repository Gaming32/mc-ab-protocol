package io.github.gaming32.mcab.and_beyond.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public abstract class Packet {
    public static enum PacketType {
        CLIENT_REQUEST(ClientRequestPacket.class), // 0
        SERVER_INFO(ServerInfoPacket.class), // 1
        BASIC_AUTH(BasicAuthPacket.class), // 2
        PLAYER_INFO(PlayerInfoPacket.class), // 3
        REMOVE_PLAYER(null), // 4
        DISCONNECT(DisconnectPacket.class), // 5
        PING(PingPacket.class), // 6
        CHUNK(ChunkPacket.class), // 7
        CHUNK_UNLOAD(UnloadChunkPacket.class), // 8
        CHUNK_UPDATE(ChunkUpdatePacket.class), // 9
        PLAYER_POS(PlayerPositionPacket.class), // 10
        ADD_VELOCITY(AddVelocityPacket.class), // 11
        CHAT(ChatPacket.class); // 12

        public final Class<? extends Packet> packetClass;

        PacketType(Class<? extends Packet> packetClass) {
            this.packetClass = packetClass;
        }
    }

    public PacketType TYPE = null;
    public abstract void read(InputStream input) throws IOException;
    public abstract void write(OutputStream output) throws IOException;

    public static Packet readPacket(InputStream input) throws IOException, InstantiationException, IllegalAccessException {
        int packetType = readUshort(input);
        if (packetType < 0) {
            return null;
        }
        Packet packet = PacketType.values()[packetType].packetClass.newInstance();
        packet.read(input);
        return packet;
    }

    public static void writePacket(Packet packet, OutputStream output) throws IOException {
        writeUshort(packet.TYPE.ordinal(), output);
        packet.write(output);
    }

    // Data readers
    protected static int readUshort(InputStream input) throws IOException {
        return input.read() + (input.read() << 8);
    }

    protected static long readVarint(InputStream input) throws IOException {
        int r = 0, i = 0, e;
        while (true) {
            e = input.read();
            r += (e & 0x7f) << (i * 7);
            if ((e & 0x80) == 0) break;
            i++;
        }
        if ((e & 0x40) != 0) {
            r |= -(1 << (i * 7) + 7);
        }
        return r;
    }

    protected static byte[] readBinary(InputStream input) throws IOException {
        byte[] buffer = new byte[(int)readVarint(input)];
        input.read(buffer);
        return buffer;
    }

    protected static String readString(InputStream input) throws IOException {
        return new String(readBinary(input), StandardCharsets.UTF_8);
    }

    protected static boolean readBoolean(InputStream input) throws IOException {
        return input.read() != 0;
    }

    protected static UUID readUUID(InputStream input) throws IOException {
        byte[] buffer = new byte[16];
        input.read(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    protected static double readDouble(InputStream input) throws IOException {
        byte[] buffer = new byte[8];
        input.read(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
    }

    // Data writers
    protected static void writeUshort(int value, OutputStream output) throws IOException {
        int head = value >> 8;
        output.write(value - head);
        output.write(head);
    }

    protected static void writeVarint(long value, OutputStream output) throws IOException {
        int b;
        while (true) {
            b = (int)(value & 0x7f);
            value >>= 7;
            if ((value == 0 && (b & 0x40) == 0) || (value == -1 && (b & 0x40) != 0)) {
                output.write(b);
                return;
            }
            output.write(0x80 | b);
        }
    }

    protected static void writeBinary(byte[] value, OutputStream output) throws IOException {
        writeVarint(value.length, output);
        output.write(value);
    }

    protected static void writeString(String value, OutputStream output) throws IOException {
        writeBinary(value.getBytes(StandardCharsets.UTF_8), output);
    }

    protected static void writeBoolean(boolean value, OutputStream output) throws IOException {
        output.write(value ? 1 : 0);
    }

    protected static void writeUUID(UUID value, OutputStream output) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(value.getMostSignificantBits());
        bb.putLong(value.getLeastSignificantBits());
        output.write(bb.array());
    }

    protected static void writeDouble(double value, OutputStream output) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(value);
        output.write(bb.array());
    }
}

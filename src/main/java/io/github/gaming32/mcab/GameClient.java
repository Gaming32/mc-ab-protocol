package io.github.gaming32.mcab;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;

import io.github.gaming32.mcab.and_beyond.WorldChunk;
import io.github.gaming32.mcab.and_beyond.packet.BasicAuthPacket;
import io.github.gaming32.mcab.and_beyond.packet.ChunkPacket;
import io.github.gaming32.mcab.and_beyond.packet.ClientRequestPacket;
import io.github.gaming32.mcab.and_beyond.packet.Packet;
import io.github.gaming32.mcab.and_beyond.packet.PlayerInfoPacket;
import io.github.gaming32.mcab.and_beyond.packet.PlayerPositionPacket;
import io.github.gaming32.mcab.and_beyond.packet.ServerInfoPacket;

public class GameClient extends SessionAdapter {
    protected final int BARRIER_BLOCK = 7754;

    protected final ProxyServer server;
    protected final ServerManager manager;
    protected final Session session;
    protected final MinecraftProtocol protocol;
    protected final ConnectionThread thread;
    protected final Map<Vector2Int, WorldChunk> loadedChunks;
    protected final Map<Long, Column> loadedColumns;
    protected String username;
    protected UUID uuid;

    protected float yaw, pitch;

    public GameClient(ProxyServer server, ServerManager manager, Session session, MinecraftProtocol protocol) {
        this.server = server;
        this.manager = manager;
        this.session = session;
        this.protocol = protocol;
        this.thread = new ConnectionThread();
        this.loadedChunks = new Hashtable<>();
        this.loadedColumns = new Hashtable<>();
    }

    public void start() {
        this.thread.start();
    }

    @Override
    public void packetReceived(PacketReceivedEvent e) {
        MinecraftProtocol proto = (MinecraftProtocol)e.getSession().getPacketProtocol();
        // System.out.println("SubProtocol: " + proto.getSubProtocol() + "\tPacket: " + e.getPacket());
        if (proto.getSubProtocol() == SubProtocol.LOGIN) {
            if (e.getPacket() instanceof LoginStartPacket) {
                LoginStartPacket packet = (LoginStartPacket)e.getPacket();
                // GameProfile profile = proto.getProfile();
                // username = profile.getName();
                // uuid = profile.getId();
                username = packet.getUsername();
                uuid = UUID.nameUUIDFromBytes(username.getBytes());
                start();
            }
        } else if (proto.getSubProtocol() == SubProtocol.GAME) {
            com.github.steveice10.packetlib.packet.Packet p = e.getPacket();
            if (p instanceof ClientPlayerPositionRotationPacket) {
                ClientPlayerPositionRotationPacket packet = (ClientPlayerPositionRotationPacket)p;
                yaw = packet.getYaw();
                pitch = packet.getPitch();
            }
        }
    }

    protected Column prepareColumn(WorldChunk chunk) {
        Column column;
        if ((column = loadedColumns.get(chunk.absX)) == null) {
            Chunk[] chunks = new Chunk[252];
            int[] biomes = new int[16128];
            column = new Column((int)chunk.absX, 0, chunks, new CompoundTag[0], new CompoundTag("heightmaps"), biomes);
            loadedColumns.put(chunk.absX, column);
        }
        convertChunk(column, chunk);
        return column;
    }

    protected Chunk convertChunk(Column column, WorldChunk chunk) {
        Chunk minecraftChunk = new Chunk();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                minecraftChunk.set(x, y, 0, BARRIER_BLOCK);
                minecraftChunk.set(x, y, 1, chunk.getTileType(x, y).minecraftID);
                minecraftChunk.set(x, y, 2, BARRIER_BLOCK);
            }
        }
        if (chunk.absY > -127 && chunk.absY < 126) {
            column.getChunks()[(int)chunk.absY + 127] = minecraftChunk;
        }
        return minecraftChunk;
    }

    protected class ConnectionThread extends Thread {
        Socket socket;
        InputStream input;
        OutputStream output;

        public ConnectionThread() {
            super("ConnectionThread-" + session.getRemoteAddress());
        }

        @Override
        public void run() {
            System.out.println("Starting connection thread " + this.getName());
            try {
                main();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                shutdown();
            }
        }

        protected void main() throws IOException, InstantiationException, IllegalAccessException {
            try {
                socket = new Socket(ProxyServer.DEST_HOST, ProxyServer.DEST_PORT);
            } catch (Exception e) {
                e.printStackTrace();
                session.disconnect("Failed to connect server", e);
                return;
            }
            input = socket.getInputStream();
            output = socket.getOutputStream();
            if (!handshake()) return;
            System.out.println("Connected to server");
            manager.connectedClients.put(session, GameClient.this);
            while (true) {
                Packet p = Packet.readPacket(input);
                if (p == null) break;
                else if (p instanceof ChunkPacket) {
                    ChunkPacket packet = (ChunkPacket)p;
                    WorldChunk chunk = packet.chunk;
                    loadedChunks.put(new Vector2Int(chunk.absX, chunk.absY), chunk);
                    Column column = prepareColumn(chunk);
                    resendColumn(column);
                } else if (p instanceof PlayerPositionPacket) {
                    PlayerPositionPacket packet = (PlayerPositionPacket)p;
                    ServerPlayerPositionRotationPacket mcPacket = new ServerPlayerPositionRotationPacket(
                        packet.x + 0.5, packet.y, 1.5, yaw, pitch, ThreadLocalRandom.current().nextInt(), false
                    );
                    session.send(mcPacket);
                }
            }
        }

        protected Packet readAndVerify(Class<? extends Packet> shouldBe) {
            Packet packet;
            try {
                packet = Packet.readPacket(input);
            } catch (Exception e) {
                e.printStackTrace();
                session.disconnect("Failed to read packet from server", e);
                return null;
            }
            if (!shouldBe.isInstance(packet)) {
                session.disconnect(
                    String.format("Server packet of type %s should be %s", packet.getClass(), shouldBe)
                );
                return null;
            }
            return packet;
        }

        protected boolean handshake() throws IOException {
            Packet packet;
            packet = new ClientRequestPacket(ProxyServer.AB_PROTOCOL_VERSION);
            Packet.writePacket(packet, output);
            KeyFactory keyFactory;
            KeyPairGenerator ec;
            try {
                keyFactory = KeyFactory.getInstance("EC");
                ec = KeyPairGenerator.getInstance("EC");
                ec.initialize(new ECGenParameterSpec("secp384r1"));
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                session.disconnect("Failed to initialize keys", e);
                e.printStackTrace();
                return false;
            }
            KeyPair keyPair = ec.generateKeyPair();
            byte[] keyBytes = keyPair.getPublic().getEncoded();
            if ((packet = readAndVerify(ServerInfoPacket.class)) == null) return false;
            ServerInfoPacket serverInfo = (ServerInfoPacket)packet;
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(serverInfo.publicKey);
            ECPublicKey serverPublicKey;
            try {
                serverPublicKey = (ECPublicKey)keyFactory.generatePublic(keySpec);
            } catch (InvalidKeySpecException e) {
                session.disconnect("Failed to load public key", e);
                e.printStackTrace();
                return false;
            }
            boolean isLocalhost = Arrays.asList("localhost", "127.0.0.1", "::1").contains(ProxyServer.DEST_HOST);
            if (serverInfo.offline) {
                packet = new BasicAuthPacket(keyBytes);
                Packet.writePacket(packet, output);
                if (isLocalhost) {
                    System.out.println("localhost connection not encrypted");
                } else {
                    encryptConnection(keyPair, serverPublicKey);
                }
                packet = new PlayerInfoPacket(uuid, username);
                Packet.writePacket(packet, output);
            } else {
                session.disconnect("Offline servers not supported yet");
                System.err.println("Offline servers not supported yet");
            }
            return true;
        }

        protected void encryptConnection(KeyPair clientKey, ECPublicKey serverPublicKey) {
            System.out.println("Encrypting connection...");
        }

        protected void resendColumn(Column column) {
            ServerChunkDataPacket packet = new ServerChunkDataPacket(column);
            session.send(packet);
        }

        protected void shutdown() {

        }
    }
}

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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;

import io.github.gaming32.mcab.and_beyond.WorldChunk;
import io.github.gaming32.mcab.and_beyond.WorldChunk.BlockType;
import io.github.gaming32.mcab.and_beyond.packet.BasicAuthPacket;
import io.github.gaming32.mcab.and_beyond.packet.ChatPacket;
import io.github.gaming32.mcab.and_beyond.packet.ChunkPacket;
import io.github.gaming32.mcab.and_beyond.packet.ChunkUpdatePacket;
import io.github.gaming32.mcab.and_beyond.packet.ClientRequestPacket;
import io.github.gaming32.mcab.and_beyond.packet.DisconnectPacket;
import io.github.gaming32.mcab.and_beyond.packet.Packet;
import io.github.gaming32.mcab.and_beyond.packet.PlayerInfoPacket;
import io.github.gaming32.mcab.and_beyond.packet.PlayerPositionPacket;
import io.github.gaming32.mcab.and_beyond.packet.ServerInfoPacket;
import io.github.gaming32.mcab.and_beyond.packet.SimplePlayerPositionPacket;
import io.github.gaming32.mcab.and_beyond.packet.UnloadChunkPacket;
import net.kyori.adventure.text.Component;

public class GameClient extends SessionAdapter {
    protected final int BARRIER_BLOCK = 7754;

    protected final ProxyServer server;
    protected final ServerManager manager;
    protected final Session session;
    protected final MinecraftProtocol protocol;
    protected final ConnectionThread thread;
    protected final Map<Vector2Int, WorldChunk> loadedChunks;
    protected final Map<Long, Column> loadedColumns;
    protected final BlockingDeque<Packet> packetsToSend;
    protected String username;
    protected UUID uuid;

    protected double x, y, z = 1.5;
    protected float yaw, pitch = 90;
    protected BlockType curItem = BlockType.STONE;

    public GameClient(ProxyServer server, ServerManager manager, Session session, MinecraftProtocol protocol) {
        this.server = server;
        this.manager = manager;
        this.session = session;
        this.protocol = protocol;
        this.thread = new ConnectionThread();
        this.loadedChunks = new Hashtable<>();
        this.loadedColumns = new Hashtable<>();
        this.packetsToSend = new LinkedBlockingDeque<>();
    }

    public void start() {
        this.thread.start();
    }

    @Override
    public void packetReceived(PacketReceivedEvent e) {
        MinecraftProtocol proto = (MinecraftProtocol)e.getSession().getPacketProtocol();
        if (proto.getSubProtocol() == SubProtocol.LOGIN) {
            if (e.getPacket() instanceof LoginStartPacket) {
                LoginStartPacket packet = (LoginStartPacket)e.getPacket();
                username = packet.getUsername();
                uuid = UUID.nameUUIDFromBytes(username.getBytes());
                start();
            }
        } else if (proto.getSubProtocol() == SubProtocol.GAME) {
            com.github.steveice10.packetlib.packet.Packet p = e.getPacket();
            if (p instanceof ClientPlayerPositionRotationPacket) {
                ClientPlayerPositionRotationPacket packet = (ClientPlayerPositionRotationPacket)p;
                x = packet.getX();
                y = packet.getY();
                yaw = packet.getYaw();
                pitch = packet.getPitch();
                SimplePlayerPositionPacket abPacket = new SimplePlayerPositionPacket(x - 0.5, y);
                try {
                    packetsToSend.putLast(abPacket);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } else if (p instanceof ClientPlayerPositionPacket) {
                ClientPlayerPositionPacket packet = (ClientPlayerPositionPacket)p;
                x = packet.getX();
                y = packet.getY();
                SimplePlayerPositionPacket abPacket = new SimplePlayerPositionPacket(x - 0.5, y);
                try {
                    packetsToSend.putLast(abPacket);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } else if (p instanceof ClientChatPacket) {
                ClientChatPacket packet = (ClientChatPacket)p;
                ChatPacket abPacket = new ChatPacket(packet.getMessage());
                try {
                    packetsToSend.putLast(abPacket);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } else if (p instanceof ClientPlayerActionPacket) {
                ClientPlayerActionPacket packet = (ClientPlayerActionPacket)p;
                if (packet.getAction() != PlayerAction.START_DIGGING) return;
                updateBlock(packet.getPosition(), BlockType.AIR);
            } else if (p instanceof ClientPlayerChangeHeldItemPacket) {
                ClientPlayerChangeHeldItemPacket packet = (ClientPlayerChangeHeldItemPacket)p;
                switch (packet.getSlot()) {
                    case 0: curItem = BlockType.STONE; break;
                    case 1: curItem = BlockType.DIRT; break;
                    case 2: curItem = BlockType.GRASS; break;
                    case 3: curItem = BlockType.WOOD; break;
                    case 4: curItem = BlockType.PLANKS; break;
                    case 5: curItem = BlockType.LEAVES; break;
                    case 8: {
                        curItem = BlockType.LEAVES;
                        ServerPlayerChangeHeldItemPacket resp = new ServerPlayerChangeHeldItemPacket(5);
                        session.send(resp);
                    } break;
                    default: {
                        curItem = BlockType.STONE;
                        ServerPlayerChangeHeldItemPacket resp = new ServerPlayerChangeHeldItemPacket(0);
                        session.send(resp);
                    } break;
                }
            } else if (p instanceof ClientPlayerPlaceBlockPacket) {
                ClientPlayerPlaceBlockPacket packet = (ClientPlayerPlaceBlockPacket)p;
                Position pos = addFaceToPosition(packet.getPosition(), packet.getFace());
                updateBlock(pos, curItem);
            }
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        thread.running = false;
        manager.connectedClients.remove(event.getSession());
        System.out.println(event.getSession() + " disconnected: " + event.getReason());
    }

    protected Position addFaceToPosition(Position a, BlockFace b) {
        return sumOfPositions(a, faceToIdentityPosition(b));
    }

    protected Position sumOfPositions(Position a, Position b) {
        return new Position(a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

    protected Position faceToIdentityPosition(BlockFace face) {
        switch (face) {
            case UP: return new Position(0, 1, 0);
            case DOWN: return new Position(0, -1, 0);
            case SOUTH: return new Position(0, 0, 1);
            case NORTH: return new Position(0, 0, -1);
            case EAST: return new Position(1, 0, 0);
            case WEST: return new Position(-1, 0, 0);
            default: return null;
        }
    }

    protected void updateBlock(Position where, BlockType block) {
        updateBlock(where, block, true);
    }

    protected void updateBlock(Position where, BlockType block, boolean send) {
        int z = where.getZ();
        if (z != 1) {
            if (send && (z == 0 || z == 2)) {
                ServerBlockChangePacket response = new ServerBlockChangePacket(
                    new BlockChangeRecord(where, BARRIER_BLOCK)
                );
                session.send(response);
            }
            return;
        }
        int x = where.getX();
        int y = where.getY();
        long cx = x >> 4;
        long cy = y >> 4;
        int bx = (int)(x - (cx << 4));
        int by = (int)(y - (cy << 4));
        WorldChunk chunk;
        if ((chunk = loadedChunks.get(new Vector2Int(cx, cy))) != null) {
            chunk.setTileType(bx, by, block);
        }
        if (cy > -128 && cy < 125) {
            Column column;
            if ((column = loadedColumns.get(cx)) != null) {
                Chunk mcChunk = column.getChunks()[(int)cy + 127];
                if (mcChunk != null) {
                    mcChunk.set(bx, by, 1, block.minecraftID);
                }
            }
        }
        if (send) {
            ChunkUpdatePacket abPacket = new ChunkUpdatePacket(cx, cy, bx, by, block);
            try {
                packetsToSend.putLast(abPacket);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
        if (chunk.absY > -128 && chunk.absY < 125) {
            column.getChunks()[(int)chunk.absY + 127] = minecraftChunk;
        }
        return minecraftChunk;
    }

    protected class ConnectionThread extends Thread {
        Socket socket;
        InputStream input;
        OutputStream output;
        SendPacketsThread sendPacketsThread;

        public volatile boolean running;

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
            running = true;
            input = socket.getInputStream();
            output = socket.getOutputStream();
            if (!handshake()) return;
            System.out.println("Connected to server");
            session.send(new ServerSetSlotPacket(0, 0, 36, new ItemStack(BlockType.STONE.minecraftItemID)));
            session.send(new ServerSetSlotPacket(0, 0, 37, new ItemStack(BlockType.DIRT.minecraftItemID)));
            session.send(new ServerSetSlotPacket(0, 0, 38, new ItemStack(BlockType.GRASS.minecraftItemID)));
            session.send(new ServerSetSlotPacket(0, 0, 39, new ItemStack(BlockType.WOOD.minecraftItemID)));
            session.send(new ServerSetSlotPacket(0, 0, 40, new ItemStack(BlockType.PLANKS.minecraftItemID)));
            session.send(new ServerSetSlotPacket(0, 0, 41, new ItemStack(BlockType.LEAVES.minecraftItemID)));
            manager.connectedClients.put(session, GameClient.this);
            sendPacketsThread = new SendPacketsThread();
            sendPacketsThread.start();
            while (running) {
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
                    if (packet.player.equals(uuid)) {
                        x = packet.x + 0.5;
                        y = packet.y;
                        ServerPlayerPositionRotationPacket mcPacket = new ServerPlayerPositionRotationPacket(
                            x, y, z, 0, 0, 0, false, PositionElement.YAW, PositionElement.PITCH
                        );
                        session.send(mcPacket);
                    }
                } else if (p instanceof SimplePlayerPositionPacket) {
                    SimplePlayerPositionPacket packet = (SimplePlayerPositionPacket)p;
                    x = packet.x + 0.5;
                    y = packet.y;
                    ServerPlayerPositionRotationPacket mcPacket = new ServerPlayerPositionRotationPacket(
                        x, y, z, 0, 0, 0, false, PositionElement.YAW, PositionElement.PITCH
                    );
                    session.send(mcPacket);
                } else if (p instanceof ChatPacket) {
                    ChatPacket packet = (ChatPacket)p;
                    ServerChatPacket mcPacket = new ServerChatPacket(Component.text(packet.message));
                    session.send(mcPacket);
                } else if (p instanceof ChunkUpdatePacket) {
                    ChunkUpdatePacket packet = (ChunkUpdatePacket)p;
                    int x = (int)(packet.cx << 4) + packet.bx;
                    int y = (int)(packet.cy << 4) + packet.by;
                    updateBlock(new Position(x, y, 1), packet.block, false);
                    ServerBlockChangePacket mcPacket = new ServerBlockChangePacket(
                        new BlockChangeRecord(new Position(x, y, 1), packet.block.minecraftID)
                    );
                    session.send(mcPacket);
                } else if (p instanceof UnloadChunkPacket) {
                    UnloadChunkPacket packet = (UnloadChunkPacket)p;
                    Vector2Int pos = new Vector2Int(packet.x, packet.y);
                    loadedChunks.remove(pos);
                    if (packet.y > -128 && packet.y < 125) {
                        Column column;
                        if ((column = loadedColumns.get(packet.x)) != null) {
                            column.getChunks()[(int)packet.y + 127] = null;
                            boolean empty = true;
                            for (Chunk mcChunk : column.getChunks()) {
                                if (mcChunk != null) {
                                    empty = false;
                                    break;
                                }
                            }
                            if (empty) {
                                loadedColumns.remove(packet.x);
                                ServerUnloadChunkPacket mcPacket = new ServerUnloadChunkPacket((int)packet.x, 1);
                                session.send(mcPacket);
                            } else {
                                resendColumn(column);
                            }
                        }
                    }
                } else if (p instanceof DisconnectPacket) {
                    DisconnectPacket packet = (DisconnectPacket)p;
                    ServerDisconnectPacket mcPacket = new ServerDisconnectPacket(Component.text(packet.reason));
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
            if (sendPacketsThread != null) {
                sendPacketsThread.interrupt();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            loadedColumns.clear();
            loadedChunks.clear();
        }

        protected class SendPacketsThread extends Thread {
            public SendPacketsThread() {
                super("SendPacketsThread-" + session.getRemoteAddress());
            }

            public void run() {
                Packet packet;
                while (running) {
                    try {
                        packet = packetsToSend.takeFirst();
                    } catch (InterruptedException e1) {
                        break;
                    }
                    synchronized (output) {
                        try {
                            Packet.writePacket(packet, output);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}

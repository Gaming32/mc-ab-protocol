package io.github.gaming32.mcab;

import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerBoundEvent;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.ServerClosingEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;

public class ServerManager extends ServerAdapter {
    public final Map<Session, GameClient> connectedClients;
    protected final Map<UUID, Integer> remotePlayers;
    protected int nextEntityId = 1;

    public ServerManager() {
        connectedClients = new Hashtable<>();
        remotePlayers = new Hashtable<>();
    }

    @Override
    public void serverBound(ServerBoundEvent e) {
        System.out.printf("Listening on %s:%d\n", e.getServer().getHost(), e.getServer().getPort());
    }

    @Override
    public void serverClosing(ServerClosingEvent e) {
        System.out.println("Closing server");
    }

    @Override
    public void serverClosed(ServerClosedEvent e) {
        System.out.println("Server closed");
    }

    @Override
    public void sessionAdded(SessionAddedEvent e) {
        MinecraftProtocol proto = (MinecraftProtocol)e.getSession().getPacketProtocol();
        e.getSession().addListener(new GameClient((ProxyServer)e.getServer(), this, e.getSession(), proto));
    }

    public int getRemotePlayerEntityId(UUID player) {
        Integer eid = remotePlayers.get(player);
        if (eid == null) {
            eid = nextEntityId++;
            remotePlayers.put(player, eid);
        }
        return eid;
    }
}

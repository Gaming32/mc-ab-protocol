package io.github.gaming32.mcab;

import java.util.Hashtable;
import java.util.Map;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerBoundEvent;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.ServerClosingEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;

public class ServerManager extends ServerAdapter {
    public final Map<Session, GameClient> connectedClients;

    public ServerManager() {
        connectedClients = new Hashtable<>();
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
}

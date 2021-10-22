package io.github.gaming32.mcab;

import java.util.HashMap;
import java.util.Map;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;

public class GameClientManager extends SessionAdapter {
    public final Map<Session, GameClient> connectedClients;

    public GameClientManager() {
        connectedClients = new HashMap<>();
    }
}

package xyz.rgnt.crownedhelper.natives.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;

public class DummyPlayerConnection
        extends PlayerConnection {

    public DummyPlayerConnection(MinecraftServer server,
                                 NetworkManager connection,
                                 EntityPlayer player) {
        super(server, connection, player);
    }



}

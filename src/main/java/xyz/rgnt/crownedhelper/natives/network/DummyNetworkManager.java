package xyz.rgnt.crownedhelper.natives.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;


public class DummyNetworkManager
    extends Connection {

    public DummyNetworkManager(PacketFlow side) {
        super(side);
    }
}

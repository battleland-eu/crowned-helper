package xyz.rgnt.crownedhelper.natives.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.EnumProtocolDirection;

public class DummyNetworkManager
    extends NetworkManager {

    public DummyNetworkManager(EnumProtocolDirection side) {
        super(side);
    }


}

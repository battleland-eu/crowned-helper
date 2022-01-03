package xyz.rgnt.crownedhelper.natives.level;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.authlib.GameProfile;
import net.minecraft.BlockUtil;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.crownedhelper.natives.network.DummyNetworkManager;
import xyz.rgnt.crownedhelper.natives.network.DummyPlayerConnection;

public class DummyEntityPlayer
        extends EntityPlayer {

    public DummyEntityPlayer(MinecraftServer server,
                             WorldServer world,
                             GameProfile profile) {
        super(server, world, profile);
        this.b = new DummyPlayerConnection(server, new DummyNetworkManager(EnumProtocolDirection.a), this);
    }

    public static @NotNull DummyEntityPlayer make(final PlayerProfile playerProfile) {
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        return new DummyEntityPlayer(server, server.E(), ((CraftPlayerProfile) playerProfile).getGameProfile());
    }


}

package xyz.rgnt.crownedhelper.api;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutCustomPayload;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftVector;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class MarkerAPI {

    public static void showMarker(final Player player,
                                  final Location location,
                                  @Nullable String name,
                                  int lifetimeTicks,
                                  final int color) {

        final EntityPlayer nativePlayer = ((CraftPlayer) player).getHandle();

        final PacketDataSerializer packetData =
                new PacketDataSerializer(Unpooled.buffer());
        packetData.a(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .writeInt(color);
        packetData.a(name == null ? "" : name);
        packetData.writeInt(lifetimeTicks);


        final var packet = new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.n, packetData);
        nativePlayer.b.sendPacket(packet);

    }
}

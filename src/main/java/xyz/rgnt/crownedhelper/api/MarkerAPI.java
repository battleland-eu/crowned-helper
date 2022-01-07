package xyz.rgnt.crownedhelper.api;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class MarkerAPI {

    public static void showMarker(final Player player,
                                  final Location location,
                                  @Nullable String name,
                                  int lifetimeTicks,
                                  final int color) {

        final ServerPlayer nativePlayer = ((CraftPlayer) player).getHandle();

        final FriendlyByteBuf packetData =
                new FriendlyByteBuf(Unpooled.buffer());
        packetData.writeBlockPos(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .writeInt(color);
        packetData.writeUtf(name == null ? "" : name);
        packetData.writeInt(lifetimeTicks);


        final var packet = new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER, packetData);
        nativePlayer.connection.send(packet);

    }
}

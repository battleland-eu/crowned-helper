package xyz.rgnt.crownedhelper.natives.network;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftVector;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.Mth;

public class OverridenPlayerConnection
        extends ServerGamePacketListenerImpl {

    @Getter
    @Setter
    private boolean overrideControls = false;

    public OverridenPlayerConnection(ServerGamePacketListenerImpl connection) {
        super(MinecraftServer.getServer(), connection.connection, connection.player);
    }


    @Override
    public void handlePlayerInput(@NotNull ServerboundPlayerInputPacket packet) {
        super.handlePlayerInput(packet);
        if (!overrideControls)
            return;

        var player = getCraftPlayer().getHandle();
        if (!(player.getVehicle() instanceof Mob vehicle))
            return;

        float forward
                = packet.getZza() * vehicle.getSpeed();
        float sideway
                = packet.getXxa() * vehicle.getSpeed();

        var forwardMovement
                = Mth.polarToCartesian(forward, Mth.yawToDeg(player.getYHeadRot()));
        forwardMovement.setZ(forwardMovement.getY());
        forwardMovement.setY(0);

        var sidewayMovement = Mth.polarToCartesian(sideway, Mth.yawToDeg(player.getYHeadRot()) + 90);
        sidewayMovement.setZ(sidewayMovement.getY());
        sidewayMovement.setY(0);

        getCraftPlayer().sendMessage("F: " + forwardMovement.toString());
        getCraftPlayer().sendMessage("S:" + sidewayMovement.toString());
        vehicle.move(MoverType.SELF, CraftVector.toNMS(sidewayMovement.add(forwardMovement)));
        vehicle.setXRot(player.getXRot() - 10);
        vehicle.setYRot(player.getYRot());
        vehicle.setYHeadRot(player.getYHeadRot());
    }
}

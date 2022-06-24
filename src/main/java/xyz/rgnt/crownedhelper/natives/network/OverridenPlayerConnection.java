package xyz.rgnt.crownedhelper.natives.network;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftVector;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.Mth;

public class OverridenPlayerConnection
        extends ServerGamePacketListenerImpl {

    @Getter
    @Setter
    private boolean overrideControls = false;

    @Getter
    @Setter
    private boolean isControlled = true;

    public OverridenPlayerConnection(ServerGamePacketListenerImpl connection) {
        super(MinecraftServer.getServer(), connection.connection, connection.player);
    }


    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
        if(isControlled)
            return;
        super.handleMovePlayer(packet);

        var player = getCraftPlayer().getHandle();
        if(!(player.getVehicle() instanceof ServerPlayer target))
            return;
        target.connection.handleMovePlayer(packet);
    }

    @Override
    public void handlePlayerInput(@NotNull ServerboundPlayerInputPacket packet) {
        super.handlePlayerInput(packet);
        if (!overrideControls)
            return;

        var player = getCraftPlayer().getHandle();
        if (!(player.getVehicle() instanceof Mob vehicle))
            return;

        double forward
                = packet.getZza() * vehicle.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double sideway
                = packet.getXxa() * vehicle.getAttributeValue(Attributes.MOVEMENT_SPEED);

        var forwardMovement
                = Mth.polarToCartesian(forward, Mth.yawToDeg(player.getYHeadRot()));
        forwardMovement.setZ(forwardMovement.getY());
        forwardMovement.setY(0);
        var sidewayMovement = Mth.polarToCartesian(sideway, Mth.yawToDeg(player.getYHeadRot()) - 90);
        sidewayMovement.setZ(sidewayMovement.getY());
        sidewayMovement.setY(0);

        if(!vehicle.isOnGround() && packet.isJumping())
            forwardMovement.setY(0.2);

        vehicle.move(MoverType.SELF,CraftVector.toNMS(forwardMovement.add(sidewayMovement)));
        vehicle.setYHeadRot(player.getYHeadRot());
        vehicle.setXRot(player.getXRot());
    }
}

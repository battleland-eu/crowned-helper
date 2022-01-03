package xyz.rgnt.crownedhelper.natives.network;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftVector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import xyz.rgnt.mth.Mth;

public class OverridenPlayerConnection
        extends PlayerConnection {

    @Getter
    @Setter
    private boolean overrideControls = false;

    public OverridenPlayerConnection(PlayerConnection connection) {
        super(MinecraftServer.getServer(), connection.a, connection.b);
    }

    @Override
    public void a(PacketPlayInArmAnimation packet) {
        super.a(packet);

        if (!overrideControls)
            return;
        var player = getCraftPlayer().getHandle();
        var vehicle = (EntityInsentient) player.getVehicle();
        if (vehicle == null)
            return;
        vehicle.swingHand(packet.b());
    }

    @Override
    public void a(PacketPlayInEntityAction packet) {
        if (!overrideControls) {
            super.a(packet);
            return;
        }

        var player = getCraftPlayer().getHandle();
        var vehicle = (EntityInsentient) player.getVehicle();
        if (vehicle == null)
            return;

        switch (packet.c()) {
            case a:
                vehicle.setSneaking(true);
                break;
            case b:
                vehicle.setSneaking(false);
                break;
        }
    }


    @Override
    public void a(PacketPlayInSteerVehicle packet) {
        super.a(packet);
        if (!overrideControls)
            return;

        var player = getCraftPlayer().getHandle();
        var vehicle = (EntityInsentient) player.getVehicle();
        if (vehicle == null)
            return;

        float forward = packet.c() * vehicle.ew();
        float sideway = packet.b() * vehicle.ew();

        var forwardMovement = Mth.polarToCartesian(forward, Mth.yawToDeg(player.getHeadRotation()));
        forwardMovement.setZ(forwardMovement.getY());
        forwardMovement.setY(0);

        var sidewayMovement = Mth.polarToCartesian(sideway, Mth.yawToDeg(player.getHeadRotation()) + 90);
        sidewayMovement.setZ(sidewayMovement.getY());
        sidewayMovement.setY(0);

        getCraftPlayer().sendMessage("F: "+ forwardMovement.toString());
        getCraftPlayer().sendMessage("S:" + sidewayMovement.toString());
        vehicle.move(EnumMoveType.a, CraftVector.toNMS(sidewayMovement.add(forwardMovement)));
        vehicle.setXRot(player.getXRot() - 10);
        vehicle.setYRot(player.getYRot());
        vehicle.setHeadRotation(player.getHeadRotation());
    }
}

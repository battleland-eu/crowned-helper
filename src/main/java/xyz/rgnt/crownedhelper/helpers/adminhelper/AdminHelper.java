package xyz.rgnt.crownedhelper.helpers.adminhelper;

import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.UUIDArgument;
import cloud.commandframework.bukkit.arguments.selector.SingleEntitySelector;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.bukkit.parsers.selector.SingleEntitySelectorArgument;
import cloud.commandframework.paper.PaperCommandManager;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.authlib.GameProfile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.crownedhelper.Plugin;
import xyz.rgnt.crownedhelper.abstraction.IControllable;
import xyz.rgnt.crownedhelper.natives.level.DummyEntityPlayer;
import xyz.rgnt.crownedhelper.natives.network.DummyNetworkManager;
import xyz.rgnt.crownedhelper.natives.network.OverridenPlayerConnection;

import java.util.UUID;

public class AdminHelper
        implements IControllable, Listener {


    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, Plugin.getInstance());
    }

    @Override
    public void terminate() {

    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        final var player = ((CraftPlayer)  event.getPlayer()).getHandle();
        player.b = new OverridenPlayerConnection(player.b);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handleMoveEvent(final PlayerMoveEvent event) {

    }

    @Override
    public void registerCommands(@NotNull PaperCommandManager<CommandSender> manager) {
        // ride command
        {
            Bukkit.getPluginManager().addPermission(new Permission("crownedhelper.command.ride", PermissionDefault.OP));
            final var builder = manager.commandBuilder("mount")
                    .permission("crownedhelper.command.ride")
                    .argument(SingleEntitySelectorArgument.optional("target"))
                    .flag(manager.flagBuilder("no-ai"))
                    .flag(manager.flagBuilder("toggle").withAliases("t"))
                    .handler((ctx) -> {
                        final var sender = (Player) ctx.getSender();
                        if(sender.getVehicle() != null
                                && ctx.flags().hasFlag("toggle")) {
                            sender.getVehicle().eject();
                            sender.sendMessage(Component.text("Unmonted.").color(NamedTextColor.GREEN));
                            return;
                        }

                        final var targetOpt = ctx.<SingleEntitySelector>getOptional("target");

                        Entity target = null;
                        if(targetOpt.isPresent())
                            target = targetOpt.get().getEntity();
                        if(sender.getGameMode()
                                .equals(GameMode.SPECTATOR))
                            target = sender.getSpectatorTarget();
                        if(target == null)
                            target = sender.getTargetEntity(12);
                        if(target == null) {
                            sender.sendMessage(Component.text("Invalid entity").color(NamedTextColor.RED));
                            return;
                        }
                        if(ctx.flags().hasFlag("no-ai") && target instanceof Mob mob)
                            Bukkit.getMobGoals()
                                    .removeAllGoals(mob);
                        target.addPassenger(sender);
                        sender.sendMessage(Component.text("Mounted.").color(NamedTextColor.GREEN));
                    });
            manager.command(builder);
        }

        // overridecontrols command
        {
            Bukkit.getPluginManager().addPermission(new Permission("crownedhelper.command.overridecontrols", PermissionDefault.OP));
            final var builder = manager.commandBuilder("overridecontrols")
                    .permission("crownedhelper.command.overridecontrols")
                    .handler((ctx) -> {
                        final var player = ((CraftPlayer) ctx.getSender()).getHandle();
                        if(!(player.b instanceof OverridenPlayerConnection connection))
                            return;
                        if(connection.isOverrideControls()) {
                            connection.setOverrideControls(false);
                            ctx.getSender().sendMessage(Component.text("Controls reverted to original handler").color(NamedTextColor.GREEN));
                        } else {
                            connection.setOverrideControls(true);
                            ctx.getSender().sendMessage(Component.text("Controls reverted to overriden handler").color(NamedTextColor.GREEN));
                        }
                    });
            manager.command(builder);
        }


        // kamaratcommand
        {
            Bukkit.getPluginManager().addPermission(new Permission("crownedhelper.command.kamarat", PermissionDefault.OP));
            final var builder = manager.commandBuilder("kamarat")
                    .permission("crownedhelper.command.ride")
                    .argument(StringArgument.of("name"))
                    .flag(manager.flagBuilder("uuid").withArgument(UUIDArgument.of("uuid")))
                    .handler((ctx) -> {
                        try {
                            final var senderPlayer = (Player) ctx.getSender();
                            final var senderNative = ((CraftPlayer) ctx.getSender()).getHandle().getWorldServer();
                            final var flags = ctx.flags();
                            final var server = (((CraftServer) Bukkit.getServer()).getServer());

                            final var profile = new CraftPlayerProfile(flags.hasFlag("uuid") ?
                                    flags.<UUID>get("uuid") :
                                    EntityPlayer.getOfflineUUID(ctx.get("name")), ctx.get("name"));

                            final var nativePlayer
                                    = DummyEntityPlayer.make(profile);

                            server.getPlayerList().j.add(nativePlayer);
                            server.getPlayerList().a(new DummyNetworkManager(EnumProtocolDirection.a), nativePlayer);
                            nativePlayer.spawnIn(senderNative.getLevel());
                            senderPlayer.teleport(nativePlayer.getBukkitEntity());


                        } catch (Exception x) {
                            ctx.getSender().sendMessage(Component.text("Nepodarilo sa: " + x).color(NamedTextColor.RED));
                            x.printStackTrace();
                        }

                    });
            manager.command(builder);
        }

    }
}

package xyz.rgnt.crownedhelper.helpers.adminhelper;

import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.UUIDArgument;
import cloud.commandframework.bukkit.arguments.selector.SingleEntitySelector;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.bukkit.parsers.selector.SingleEntitySelectorArgument;
import cloud.commandframework.paper.PaperCommandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
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
import xyz.rgnt.crownedhelper.Plugin;
import xyz.rgnt.crownedhelper.abstraction.IControllable;

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
        player.connection = new OverridenPlayerConnection(player.connection);
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
                        if(!(player.connection instanceof OverridenPlayerConnection connection))
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


    }
}

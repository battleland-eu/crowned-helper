package xyz.rgnt.crownedhelper;

import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.A;
import xyz.rgnt.crownedhelper.helpers.adminhelper.AdminHelper;
import xyz.rgnt.crownedhelper.helpers.commandqueue.CommandQueueManager;

import java.util.function.Function;

@Log4j2
public final class Plugin
        extends JavaPlugin {

    @Getter
    private static Plugin instance;
    {
        instance = this;
    }


    @Getter
    private PaperCommandManager<CommandSender> commandManager;

    private AdminHelper adminHelper;
    private CommandQueueManager commandQueue;

    @Override
    public void onLoad() {
        this.commandQueue = new CommandQueueManager(this);
        this.adminHelper  = new AdminHelper();
    }

    @Override
    public void onEnable() {
        try {
            this.commandManager = new PaperCommandManager<>(this,
                    AsynchronousCommandExecutionCoordinator.simpleCoordinator(),
                    Function.identity(),
                    Function.identity()
            );
            if(this.commandManager.queryCapability(CloudBukkitCapabilities.BRIGADIER))
                this.commandManager.registerBrigadier();
            if(this.commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
                this.commandManager.registerAsynchronousCompletions();
        } catch (Exception e) {
            e.printStackTrace();
        }
        {
            Permission commandPermission = new Permission("crownedhelper.command.crownedhelper", PermissionDefault.OP);
            Bukkit.getPluginManager().addPermission(commandPermission);

            final var builder = commandManager.commandBuilder("crownedhelper", "ch")
                    .permission(commandPermission.getName());


        }

        this.commandQueue.initialize();
        this.commandQueue.registerCommands(this.commandManager);

        this.adminHelper.initialize();
        this.adminHelper.registerCommands(this.commandManager);
    }

    @Override
    public void onDisable() {
        this.adminHelper.terminate();
        this.commandQueue.terminate();
    }
}

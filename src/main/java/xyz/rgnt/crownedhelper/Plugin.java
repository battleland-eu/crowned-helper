package xyz.rgnt.crownedhelper;

import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
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

    private CommandQueueManager commandQueue;

    @Override
    public void onLoad() {
        this.commandQueue = new CommandQueueManager(this);
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

        this.commandQueue.initialize();
        this.commandQueue.registerCommands(this.commandManager);
    }

    @Override
    public void onDisable() {
        this.commandQueue.terminate();
    }
}

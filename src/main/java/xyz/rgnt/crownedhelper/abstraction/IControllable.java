package xyz.rgnt.crownedhelper.abstraction;

import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public interface IControllable {

    void initialize() throws Exception;
    void terminate()  throws Exception;

    default void registerCommands(final @NotNull PaperCommandManager<CommandSender> manager) {

    }
}

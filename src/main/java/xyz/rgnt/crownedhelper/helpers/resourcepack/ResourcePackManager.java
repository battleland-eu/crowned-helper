package xyz.rgnt.crownedhelper.helpers.resourcepack;

import cloud.commandframework.paper.PaperCommandManager;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.crownedhelper.abstraction.IControllable;

public class ResourcePackManager
    implements IControllable {

    @Getter
    private JsonObject configuration;


    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void terminate() throws Exception {

    }

    @Override
    public void registerCommands(@NotNull PaperCommandManager<CommandSender> manager) {
        IControllable.super.registerCommands(manager);
    }
}

package xyz.rgnt.crownedhelper.helpers.commandqueue.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import xyz.rgnt.crownedhelper.Plugin;
import xyz.rgnt.crownedhelper.statics.TimeStatics;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Setter
public class QueuedCommand {

    private long timestamp;
    private String command;
    private int executionDelay;
    private ExecSide executionSide;

    private String[] allowedWorlds;
    private String[] blockedWorlds;

    private volatile String lastFailure = "";

    public QueuedCommand(long timestamp,
                         String command,
                         int executionDelay,
                         ExecSide executionSide,
                         String[] allowedWorlds,
                         String[] blockedWorlds) {
        this.timestamp = timestamp;
        this.command = command;
        this.executionDelay = executionDelay;
        this.executionSide = executionSide;
        this.allowedWorlds = allowedWorlds;
        this.blockedWorlds = blockedWorlds;
    }

    public QueuedCommand(JsonObject data) {
        this.timestamp = TimeStatics.getCurrentUnix();
        this.command = data.get("command").getAsString();
        this.executionDelay = data.has("delay")
                ? data.get("delay").getAsInt()
                : 0;
        this.executionSide = data.has("side")
                ? ExecSide.valueOf(data.get("side").getAsString().toUpperCase(Locale.ROOT))
                : ExecSide.CLIENT;
        if(data.has("allowed_worlds")){
            final var raw = data.get("allowed_worlds").getAsString();
            this.allowedWorlds = raw.contains(",") ? raw.split(",") : new String[] {raw};
        } else {
            this.allowedWorlds = new String[0];
        }
        if(data.has("blocked_worlds")){
            final var raw = data.get("blocked_worlds").getAsString();
            this.blockedWorlds = raw.contains(",") ? raw.split(",") : new String[] {raw};
        } else {
            this.blockedWorlds = new String[0];
        }
    }

    public void executeAs(final Plugin plugin,
                          final Player player,
                          Function<String, String> comandPreprocess,
                          Consumer<String> success,
                          Runnable failure) {
        try {
            if (!TimeStatics.deltaIsLargerThan(this.getTimestamp(),
                    this.getExecutionDelay()))
                throw new IllegalStateException("Not the time.");

            // cancel if world is blocked
            if (this.getBlockedWorlds() != null
                    && this.getBlockedWorlds().length > 0)
                if (Stream.of(this.getBlockedWorlds())
                        .anyMatch((world) -> player.getWorld().getName().equals(world)))
                    throw new IllegalStateException("Blocked world.");
            // cancel if world isnt allowed
            if (this.getAllowedWorlds() != null
                    && this.getAllowedWorlds().length > 0)
                if (Stream.of(this.getAllowedWorlds())
                        .noneMatch((world) -> player.getWorld().getName().equals(world)))
                    throw new IllegalStateException("No allowed world.");

            final var processedCommand = comandPreprocess.apply(this.getCommand());
            // execute this
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (this.getExecutionSide().equals(QueuedCommand.ExecSide.CLIENT))
                    player.performCommand(processedCommand);
                else
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                success.accept(processedCommand);
            });

        } catch (IllegalStateException x) {
            failure.run();
            this.lastFailure = x.getMessage();
        }
    }

    public long getScheduledExecution() {
        return getTimestamp() + getExecutionDelay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueuedCommand that = (QueuedCommand) o;
        return executionDelay == that.executionDelay &&
                command.equals(that.command) &&
                executionSide == that.executionSide &&
                Arrays.equals(allowedWorlds, that.allowedWorlds) &&
                Arrays.equals(blockedWorlds, that.blockedWorlds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(command, executionDelay, executionSide);
        result = 31 * result + Arrays.hashCode(allowedWorlds);
        result = 31 * result + Arrays.hashCode(blockedWorlds);
        return result;
    }

    @Override
    public String toString() {
        return "QueuedCommand\n{" +
                "\n   command='" + command + '\'' +
                ",\n   executionDelay=" + executionDelay +
                ",\n   executionSide=" + executionSide +
                ",\n   allowedWorlds=" + Arrays.toString(allowedWorlds) +
                ",\n   blockedWorlds=" + Arrays.toString(blockedWorlds) +
                "\n} (Last Failure="+this.lastFailure +")";
    }

    public enum ExecSide {
        SERVER, CLIENT
    }
}

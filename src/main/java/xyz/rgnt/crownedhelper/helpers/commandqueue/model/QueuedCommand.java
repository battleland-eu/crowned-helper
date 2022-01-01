package xyz.rgnt.crownedhelper.helpers.commandqueue.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter @Setter
public class QueuedCommand {

    private long timestamp;
    private String command;
    private int executionDelay;
    private ExecSide executionSide;

    private String[] allowedWorlds;
    private String[] blockedWorlds;

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
                "\n}";
    }

    public enum ExecSide {
        SERVER, CLIENT
    }

}

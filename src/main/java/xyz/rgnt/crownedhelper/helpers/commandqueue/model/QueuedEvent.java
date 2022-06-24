package xyz.rgnt.crownedhelper.helpers.commandqueue.model;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.function.Consumer;

public class QueuedEvent {

    @Getter
    private QueuedCommand command;
    @Getter
    private ExecCondition condition;


    public QueuedEvent(JsonObject data) {
        condition = ExecCondition.valueOf(data.get("condition").getAsString().toUpperCase(Locale.ROOT));
        command = new QueuedCommand(data.getAsJsonObject("command"));
    }


    public enum ExecCondition {
        PLAYER_CLICK_PLAYER
    }

}

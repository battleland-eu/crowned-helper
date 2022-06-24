package xyz.rgnt.crownedhelper.helpers.commandqueue;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.paper.PaperCommandManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import xyz.rgnt.crownedhelper.Plugin;
import xyz.rgnt.crownedhelper.abstraction.IControllable;
import xyz.rgnt.crownedhelper.helpers.commandqueue.model.QueuedCommand;
import xyz.rgnt.crownedhelper.helpers.commandqueue.model.QueuedEvent;
import xyz.rgnt.crownedhelper.statics.TimeStatics;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Log4j2(topic = "CommandQueue Manager")
public class CommandQueueManager
        implements CommandQueueAPI, IControllable, Listener {

    public static final @NotNull NamespacedKey COMMAND_QUEUE_DATA_ID
            = Objects.requireNonNull(NamespacedKey.fromString("crownedhelper:commandqueue_storage"));

    public static final @NotNull NamespacedKey COMMAND_QUEUE_DEFAULT_BRANCH
            = Objects.requireNonNull(NamespacedKey.fromString("crownedhelper:default_branch"));

    private final Plugin plugin;
    private BukkitTask tickingTask;

    private final Map<UUID, Map<NamespacedKey, LinkedList<QueuedCommand>>> commands
            = new HashMap<>();

    private final Map<QueuedEvent.ExecCondition, LinkedList<QueuedEvent>> events
            = new HashMap<>();

    public CommandQueueManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        {
            final var eventsConfig =
                    JsonParser.parseReader(
                                    new InputStreamReader(plugin.getResource("resources/commandqueue/handlers.json"), StandardCharsets.UTF_8)
                            )
                            .getAsJsonObject();

            eventsConfig.entrySet().forEach(entry -> {
                JsonObject json = entry.getValue().getAsJsonObject();
                final var event = new QueuedEvent(json);
                this.events.compute(event.getCondition(), (cond, handlers) -> {
                    if (handlers == null)
                        handlers = new LinkedList<>();
                    handlers.add(event);

                    return handlers;
                });
            });
        }

        // ticking task
        tickingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            commands.forEach((userUuid, userBranches) -> {
                userBranches.forEach((branchId, userBranch) -> {
                    final var player = Bukkit.getPlayer(userUuid);
                    if (player == null || !player.isOnline())
                        return;
                    var command = userBranch.peek();
                    if (command == null)
                        return;

                    command.executeAs(plugin, player, (cmd) -> cmd, (cmd) -> {
                        userBranch.poll();
                        log.info("Executing {} command '{}' on branch '{}' for '{}'",
                                command.getExecutionSide().name().toLowerCase(),
                                cmd,
                                branchId.asString(),
                                player.getName());
                    }, () -> {
                    });

                });
            });
        }, 0, 1);
    }

    @Override
    public void terminate() {
        if (tickingTask != null)
            tickingTask.cancel();
    }

    @Override
    public @NotNull LinkedList<QueuedCommand> createBranchQueue(@NotNull Player player,
                                                                @NotNull NamespacedKey key) {
        Map<NamespacedKey, LinkedList<QueuedCommand>> branch = this.commands.compute(
                player.getUniqueId(), (uuid, originBranch) -> originBranch == null ? new HashMap<>() : originBranch
        );
        final LinkedList<QueuedCommand> branchQueue = new LinkedList<>();
        branch.put(key, branchQueue);

        return branchQueue;
    }

    @Override
    public LinkedList<QueuedCommand> retrieveBranchQueue(@NotNull Player player,
                                                         @NotNull NamespacedKey key) {
        return this.commands.getOrDefault(player.getUniqueId(), new HashMap<>()).get(key);
    }

    @Override
    public void enqueueCommand(@NotNull Player player,
                               @NotNull QueuedCommand command,
                               @Nullable NamespacedKey branchId) {
        final var branch
                = retrieveOrCreateBranchQueue(player, branchId == null ? COMMAND_QUEUE_DEFAULT_BRANCH : branchId);
        if (branch.size() > 0) {
            // schedule execution after last entry
            final var last = branch.getLast();
            if (last != null)
                command.setTimestamp(last.getScheduledExecution());
        }
        branch.add(command);
    }

    @EventHandler
    public void handlePlayerClickPlayer(final PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target))
            return;
        if (!e.getHand().equals(EquipmentSlot.HAND))
            return;
        if(!target.isOnline())
            return;

        this.events.get(QueuedEvent.ExecCondition.PLAYER_CLICK_PLAYER).forEach(event -> {
            event.getCommand().executeAs(plugin, e.getPlayer(), (cmd) -> {
                return cmd.replace("{target}", target.getName());
            }, (command) -> {
                log.debug("Executing {} command event '{}' for '{}' targeting '{}'",
                        event.getCommand().getExecutionSide().name().toLowerCase(),
                        command,
                        e.getPlayer().getName(),
                        target.getName()
                );
            }, () -> {
            });
        });
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var data = this.commands.remove(player.getUniqueId());
        if (data == null
                || data.size() == 0
                || data.values().stream().noneMatch(branch -> branch.size() > 0))
            return;
        final var cargo = player.getPersistentDataContainer();
        {
            // serialize branches to json
            final var json = new JsonObject();
            data.forEach((branchId, branch) -> {
                final var branchJson = new JsonArray();
                branch.forEach(command ->
                        branchJson.add(new Gson().toJsonTree(command))
                );

                json.add(branchId.asString(), branchJson);
            });

            // store json in player data
            cargo.set(COMMAND_QUEUE_DATA_ID, PersistentDataType.STRING, new Gson().toJson(json));
        }
        log.info("Stored data for {}", player.getName());
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var cargo = player.getPersistentDataContainer();

        final var jsonRaw
                = cargo.get(COMMAND_QUEUE_DATA_ID, PersistentDataType.STRING);
        if (jsonRaw == null)
            return;
        try {
            JsonObject json = new JsonParser()
                    .parse(jsonRaw)
                    .getAsJsonObject();

            json.entrySet().forEach((jsonEntry) -> {
                final var branchId = NamespacedKey.fromString(jsonEntry.getKey());
                if (branchId == null)
                    throw new IllegalStateException(String.format("Invalid branch name %s", jsonEntry.getKey()));
                final var branch = createBranchQueue(player, branchId);
                final var branchJson = jsonEntry.getValue().getAsJsonArray();
                branchJson.forEach((entry) -> {
                    final QueuedCommand command = new Gson().fromJson(entry, QueuedCommand.class);
                    branch.add(command);
                });
            });

            log.info("Loaded data for {}", player.getName());
            cargo.remove(COMMAND_QUEUE_DATA_ID);
        } catch (Exception x) {
            log.error("Couldn't load data '{}' for player '{}'", jsonRaw, player.getName(), x);
            cargo.remove(COMMAND_QUEUE_DATA_ID);
        }
    }

    @Override
    public void registerCommands(@NotNull PaperCommandManager<CommandSender> manager) {
        Permission commandPermission = new Permission("crownedhelper.command.commandqueue", PermissionDefault.OP);
        if (Bukkit.getPluginManager().getPermission(commandPermission.getName()) == null)
            Bukkit.getPluginManager().addPermission(commandPermission);

        final var builder = manager.commandBuilder("commandqueue", "cq")
                .permission(commandPermission.getName());

        manager.command(builder.literal("query")
                .argument(PlayerArgument.of("target"))
                .handler((ctx) -> {
                    final Player target = ctx.get("target");
                    final var sender = ctx.getSender();

                    final var message = Component.text();
                    message.append(
                            Component
                                    .text(String.format("Query of enqueued commands for player '%s'", target.getName()))
                                    .color(NamedTextColor.WHITE)
                    );
                    final var branches = this.commands.get(target.getUniqueId());
                    if (branches == null
                            || branches.size() == 0) {
                        message.append(Component.text().color(NamedTextColor.RED).content("\n    None"));
                        sender.sendMessage(message);
                        return;
                    }

                    branches.forEach((key, branch) -> {
                        message.append(Component.newline());
                        message.append(Component.text("    ").color(NamedTextColor.DARK_GRAY));
                        message.append(Component.text(key.namespace()).color(NamedTextColor.DARK_GRAY));
                        message.append(Component.text(":").color(NamedTextColor.DARK_GRAY));
                        message.append(Component.text(key.value()).color(NamedTextColor.GRAY));

                        for (QueuedCommand queuedCommand : branch) {
                            message.append(Component.text("\n        \u2192 ")
                                    .color(NamedTextColor.GREEN));
                            message.append(Component.text(queuedCommand.getCommand())
                                    .color(NamedTextColor.WHITE)
                                    .hoverEvent(Component.text(queuedCommand.toString()))
                                    .clickEvent(ClickEvent.suggestCommand("/" + queuedCommand.getCommand())));
                        }
                    });
                    sender.sendMessage(message);
                }));


        manager.command(builder.literal("enqueue")
                .argument(PlayerArgument.of("target"))
                .argument(StringArgument.<CommandSender>newBuilder("command").quoted()
                        .withDefaultDescription(ArgumentDescription.of("Command to execute surrounded with quotes, example: \"say hello!\"")))
                .argument(IntegerArgument.<CommandSender>newBuilder("delay").asOptionalWithDefault(0)
                        .withDefaultDescription(ArgumentDescription.of("Delay for command execution in seconds")))
                .flag(manager.flagBuilder("side").withAliases("s")
                        .withArgument(EnumArgument.optional(QueuedCommand.ExecSide.class, "side", QueuedCommand.ExecSide.CLIENT)))
                .flag(manager.flagBuilder("branch").withAliases("b")
                        .withArgument(StringArgument.of("branch")))
                .flag(manager.flagBuilder("allowed-worlds")
                        .withArgument(StringArgument.newBuilder("allowed-worlds")
                                        .quoted()
                                        .withDefaultDescription(ArgumentDescription.of("Comma separated list of allowed worlds"))
                                //  .withSuggestionsProvider((ctx, label) -> Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList())))
                        ))
                .flag(manager.flagBuilder("blocked-worlds")
                        .withArgument(StringArgument.newBuilder("blocked-worlds")
                                        .quoted()
                                        .withDefaultDescription(ArgumentDescription.of("Comma separated list of blocked worlds"))
                                // .withSuggestionsProvider((ctx, label) -> Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()))))
                        ))
                .handler(ctx -> {
                    final Player target = ctx.get("target");
                    // params
                    final String command = ctx.get("command");
                    final Integer delay = ctx.get("delay");

                    // flags
                    final String execBranchString = ctx.flags().getValue("branch", null);
                    final NamespacedKey execBranch;
                    {
                        if (execBranchString == null)
                            execBranch = null;
                        else
                            execBranch = NamespacedKey.fromString(execBranchString, plugin);
                    }

                    final QueuedCommand.ExecSide execSide
                            = ctx.flags().getValue("side", QueuedCommand.ExecSide.CLIENT);

                    final var allowedWorldsString = ctx.flags().getValue("allowed-worlds", "");
                    final String[] allowedWorlds
                            = allowedWorldsString.length() > 0 ? allowedWorldsString.split(",") : new String[0];

                    final var blockedWorldsString = ctx.flags().getValue("blocked-worlds", "");
                    final String[] blockedWorlds
                            = blockedWorldsString.length() > 0 ? blockedWorldsString.split(",") : new String[0];


                    final var qc = new QueuedCommand(TimeStatics.getCurrentUnix(), command, delay, execSide, allowedWorlds, blockedWorlds);
                    enqueueCommand(target, qc, execBranch);
                    ctx.getSender().sendMessage(Component.empty()
                            .append(
                                    Component
                                            .text(String.format("Enqueued command '%s' for player '%s'", command, target.getName()))
                                            .color(NamedTextColor.GRAY)

                            )
                            .hoverEvent(Component.text(qc.toString()))
                    );
                }));

        manager.command(builder.literal("dequeue")
                .argument(PlayerArgument.of("target"))
                .argument(IntegerArgument.of("command_index"))
                .handler((ctx) -> {
                    ctx.getSender().sendMessage(Component.text("To som este nenaprogramoval.").color(NamedTextColor.RED));
                }));
    }


}

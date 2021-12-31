package xyz.rgnt.crownedhelper.helpers.commandqueue;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.captions.Caption;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import netscape.javascript.JSObject;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.crownedhelper.Plugin;
import xyz.rgnt.crownedhelper.abstraction.IControllable;
import xyz.rgnt.crownedhelper.helpers.commandqueue.model.QueuedCommand;
import xyz.rgnt.crownedhelper.statics.TimeStatics;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2(topic = "CommandQueue Manager")
public class CommandQueueManager
        implements IControllable, Listener {

    private final Plugin plugin;
    private BukkitTask tickingTask;

    private final @NotNull NamespacedKey dataKey = Objects.requireNonNull(NamespacedKey.fromString("crownedhelper:commandqueue_storage"));

    private final Map<UUID, List<Queue<QueuedCommand>>> queueBranches
            = new HashMap<>();

    public CommandQueueManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // ticking task
        tickingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            queueBranches.forEach((userUuid, userBranches) -> {
                userBranches.forEach(userBranch -> {
                    final var player = Bukkit.getPlayer(userUuid);
                    if (player == null || !player.isOnline())
                        return;

                    var command = userBranch.peek();
                    if (command == null)
                        return;

                    if (TimeStatics.deltaIsLargerThan(command.getTimestamp(),
                            command.getExecutionDelay())) {

                        // cancel if world is blocked
                        if (command.getBlockedWorlds().length > 0)
                            if (Stream.of(command.getBlockedWorlds())
                                    .anyMatch((world) -> player.getWorld().getName().equals(world)))
                                return;
                        // cancel if world isnt allowed
                        if (command.getAllowedWorlds().length > 0)
                            if (Stream.of(command.getAllowedWorlds())
                                    .noneMatch((world) -> player.getWorld().getName().equals(world)))
                                return;

                        userBranch.poll();

                        // execute command
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (command.getExecutionSide().equals(QueuedCommand.ExecSide.CLIENT))
                                player.performCommand(command.getCommand());
                            else
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.getCommand());
                            log.info("Executing {} command '{}' for '{}'",
                                    command.getExecutionSide().name().toLowerCase(),
                                    command.getCommand(),
                                    player.getName());
                        });
                    }
                });
            });
        }, 0, 1);
    }

    @Override
    public void terminate() {
        if (tickingTask != null)
            tickingTask.cancel();
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var data = this.queueBranches.remove(player.getUniqueId());
        if (data == null || data.size() == 0)
            return;
        final var cargo = player.getPersistentDataContainer();
        final var json = new JsonArray();
        data.forEach(branch -> {
            final var branchJson = new JsonArray();
            branch.forEach(command -> {
                branchJson.add(new Gson().toJsonTree(command));
            });
            json.add(branchJson);
        });
        cargo.set(dataKey, PersistentDataType.STRING, new Gson().toJson(json));
        log.info("Stored data for {}", player.getName());
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var cargo = player.getPersistentDataContainer();

        String jsonRaw = cargo.get(dataKey, PersistentDataType.STRING);
        if (jsonRaw == null)
            return;
        try {
            JsonArray json = new JsonParser().parse(jsonRaw).getAsJsonArray();
            json.forEach(branchInferior -> {
                JsonArray branchJson = branchInferior.getAsJsonArray();
                final var branch = createNewQueueBranch(player);
                branchJson.forEach(commandInferior -> {
                    QueuedCommand command = new Gson().fromJson(commandInferior.getAsJsonObject(), QueuedCommand.class);
                    branch.add(command);
                });
            });
            log.info("Loaded data for {}", player.getName());
        } catch (Exception x) {
            log.error("Couldn't load data '{}' for player '{}'", jsonRaw, player.getName(), x);
        }
    }

    /**
     * @param player Player
     * @return Existing queue branch if possible, otherwise creates new queue branch.
     */
    public @NotNull Queue<QueuedCommand> createOrGetQueueBranch(final @NotNull Player player) {
        var branch = getQueueBranch(player, -1);
        if (branch == null)
            branch = createNewQueueBranch(player);
        return branch;
    }

    /**
     * @param player Player
     * @param index  Index of queue branch
     * @return Nullable queue branch of specified player. If index is -1 returns first queue branch.
     */
    public Queue<QueuedCommand> getQueueBranch(final @NotNull Player player, final int index) {
        final var branch = this.queueBranches.get(player.getUniqueId());
        if (branch == null)
            return null;
        try {
            if (index == -1)
                return branch.get(0);
            return branch.get(index);
        } catch (NoSuchElementException x) {
            return null;
        }
    }

    /**
     * @param player Player
     * @return New queue branch of specified player.
     */
    public Queue<QueuedCommand> createNewQueueBranch(final @NotNull Player player) {
        List<Queue<QueuedCommand>> branch = this.queueBranches.compute(
                player.getUniqueId(), (uuid, originBranch) -> originBranch == null ? new ArrayList<>() : originBranch
        );
        final Queue<QueuedCommand> branchQueue = new ArrayDeque<>();
        branch.add(branchQueue);

        return branchQueue;
    }

    /**
     * Enqueues command
     *
     * @param player    Player
     * @param command   Command
     * @param newBranch Whether this command should be on new queue branch
     */
    public void enqueueCommand(final @NotNull Player player,
                               final @NotNull QueuedCommand command,
                               boolean newBranch) {
        final var branch = newBranch ? createNewQueueBranch(player) : createOrGetQueueBranch(player);
        branch.add(command);
    }

    public void dequeueCommand(final @NotNull QueuedCommand command) {

    }

    @Override
    public void registerCommands(@NotNull PaperCommandManager<CommandSender> manager) {
        Permission commandPermission = new Permission("crownedhelper.command.commandqueue", PermissionDefault.OP);
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
                            Component.text(String.format("Query of enqueued commands for player '%s'", target.getName())).color(NamedTextColor.GRAY)
                    );
                    final var branches = this.queueBranches.get(target.getUniqueId());
                    if (branches == null
                            || branches.size() == 0) {
                        message.append(Component.text().color(NamedTextColor.RED).content("\n    None"));
                        sender.sendMessage(message);
                        return;
                    }
                    int branchId = 0;
                    for (Queue<QueuedCommand> branch : branches) {
                        branchId++;
                        message.append(Component.newline());
                        message.append(Component.text("    #").color(NamedTextColor.DARK_GRAY));
                        message.append(Component.text(branchId).color(NamedTextColor.WHITE));
                        message.append(Component.text(" - ").color(NamedTextColor.DARK_GRAY));

                        for (QueuedCommand queuedCommand : branch) {
                            message.append(Component.text("'").color(NamedTextColor.GRAY));
                            message.append(Component.text(String.format("%s", queuedCommand.getCommand()))
                                    .hoverEvent(Component.text(queuedCommand.toString()))
                                    .color(NamedTextColor.GREEN));
                            message.append(Component.text("'").color(NamedTextColor.GRAY));
                            message.append(Component.text(" \u2192 ").color(NamedTextColor.BLUE));
                        }
                    }
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
                .flag(manager.flagBuilder("new-branch").withAliases("b"))
                .flag(manager.flagBuilder("allowed-worlds")
                        .withArgument(StringArgument.newBuilder("allowed-worlds")
                                .quoted()
                                .withDefaultDescription(ArgumentDescription.of("Comma separated list of allowed worlds"))
                                .withSuggestionsProvider((ctx, label) -> Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()))))
                .flag(manager.flagBuilder("blocked-worlds")
                        .withArgument(StringArgument.newBuilder("blocked-worlds")
                                .quoted()
                                .withDefaultDescription(ArgumentDescription.of("Comma separated list of blocked worlds"))
                                .withSuggestionsProvider((ctx, label) -> Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()))))
                .handler(ctx -> {
                    final Player target = ctx.get("target");
                    // params
                    final String command = ctx.get("command");
                    final Integer delay = ctx.get("delay");

                    // flags
                    final boolean newBranch = ctx.flags().contains("new-branch");
                    final QueuedCommand.ExecSide runAs
                            = ctx.flags().getValue("side", QueuedCommand.ExecSide.CLIENT);

                    final var allowedWorldsString = ctx.flags().getValue("allowed-worlds", "");
                    final String[] allowedWorlds
                            = allowedWorldsString.length() > 0 ? allowedWorldsString.split(",") : new String[0];

                    final var blockedWorldsString = ctx.flags().getValue("blocked-worlds", "");
                    final String[] blockedWorlds
                            = blockedWorldsString.length() > 0 ? blockedWorldsString.split(",") : new String[0];


                    final var qc = new QueuedCommand(TimeStatics.getCurrentUnix(), command, delay, runAs, allowedWorlds, blockedWorlds);
                    enqueueCommand(target, qc, newBranch);
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

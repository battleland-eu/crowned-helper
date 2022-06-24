package xyz.rgnt.crownedhelper.helpers.commandqueue;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.crownedhelper.helpers.commandqueue.model.QueuedCommand;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public interface CommandQueueAPI {

    /**
     * Creates new branch queue with identifier.
     * @param player Player to whom the branch belongs to.
     * @param key Identifier of branch.
     * @return Branch Queue.
     */
    @NotNull LinkedList<QueuedCommand> createBranchQueue(final @NotNull Player player,
                                                         final @NotNull NamespacedKey key);

    /**
     * Retrieves branch queue with identifier
     * @param player Player to whom the branch belongs to.
     * @param key Identifier of branch.
     * @return Branch Queue, can be null.
     */
    LinkedList<QueuedCommand> retrieveBranchQueue(final @NotNull Player player,
                                             final @NotNull NamespacedKey key);

    /**
     * Retrieves or creates branch queue with identifier.
     * @param player Player to whom the branch belongs to.
     * @param key Identifier of branch
     * @return Branch Queue.
     */
    default @NotNull LinkedList<QueuedCommand> retrieveOrCreateBranchQueue(final @NotNull Player player,
                                                                      final @NotNull NamespacedKey key) {
        var branch = retrieveBranchQueue(player, key);
        return branch == null ? createBranchQueue(player ,key) : branch;
    }

    /**
     * Enqueues command. If branch is specified and does not exist, the branch will be created. If branch is not specified, default branch will be used.
     * @param player   Player to whose branch will the command be enqueued to.
     * @param command  Command to enqueue.
     * @param branchId Branch identifier.
     */
    void enqueueCommand(final @NotNull Player player,
                        final @NotNull QueuedCommand command,
                        final @Nullable NamespacedKey branchId);



}

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Backwards-compatibility shim for the pre-relocation Slimefun package.
 * <p>
 * This fork relocated Slimefun's API from {@code io.github.thebusybiscuit.slimefun4.*} to
 * {@code io.github.thebusybiscuit.slimefun5.*}. Third-party plugins (e.g. RedProtect's
 * {@code SlimefunHook}) were compiled against the old package and register a Bukkit listener for
 * {@code io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent}. Without this class
 * their listener registration fails with a {@link ClassNotFoundException}.
 * <p>
 * The real {@code io.github.thebusybiscuit.slimefun5.api.events.ExplosiveToolBreakBlocksEvent}
 * <strong>extends</strong> this class and reuses this single {@link HandlerList}. Bukkit resolves an
 * event's handlers via the (shared) {@link HandlerList}, so a listener registered against this old class
 * receives the fired {@code slimefun5} event too - because it is an {@code instanceof} this type.
 */
public class ExplosiveToolBreakBlocksEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack itemInHand;
    private final Block mainBlock;
    private final List<Block> additionalBlocks;
    private boolean cancelled;

    protected ExplosiveToolBreakBlocksEvent(Player player, Block block, List<Block> blocks, ItemStack item) {
        super(player);

        this.mainBlock = block;
        this.additionalBlocks = blocks;
        this.itemInHand = item;
    }

    /**
     * This returns the primary {@link Block} that was broken.
     *
     * @return The primary broken {@link Block}
     */
    @Nonnull
    public Block getPrimaryBlock() {
        return this.mainBlock;
    }

    /**
     * Gets the {@link Block} {@link List} of blocks destroyed in this event.
     *
     * @return The broken blocks
     */
    @Nonnull
    public List<Block> getAdditionalBlocks() {
        return this.additionalBlocks;
    }

    /**
     * Gets the {@link ItemStack} of the tool used to destroy this block.
     *
     * @return The {@link ItemStack} in the hand of the {@link Player}
     */
    @Nonnull
    public ItemStack getItemInHand() {
        return this.itemInHand;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
